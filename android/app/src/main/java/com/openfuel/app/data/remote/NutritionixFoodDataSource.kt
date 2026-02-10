package com.openfuel.app.data.remote

import com.google.gson.annotations.SerializedName
import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.domain.model.RemoteFoodSource
import java.util.Locale
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface NutritionixFoodDataSource {
    suspend fun searchByText(
        query: String,
        token: UserInitiatedNetworkToken,
    ): List<RemoteFoodCandidate>

    suspend fun lookupByBarcode(
        barcode: String,
        token: UserInitiatedNetworkToken,
    ): RemoteFoodCandidate?
}

class NutritionixRemoteFoodDataSource internal constructor(
    private val api: NutritionixApi,
    private val userInitiatedNetworkGuard: UserInitiatedNetworkGuard,
    private val appId: String,
    private val apiKey: String,
    private val remoteUserId: String = "0",
) : NutritionixFoodDataSource {
    override suspend fun searchByText(
        query: String,
        token: UserInitiatedNetworkToken,
    ): List<RemoteFoodCandidate> {
        userInitiatedNetworkGuard.validate(token)
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank() || !hasCredentials()) {
            return emptyList()
        }
        val response = api.naturalNutrients(
            appId = appId,
            apiKey = apiKey,
            remoteUserId = remoteUserId,
            request = NutritionixNaturalRequest(query = trimmedQuery),
        )
        return response.foods.orEmpty()
            .mapNotNull { food -> food.toRemoteFoodCandidate() }
            .distinctBy { candidate ->
                candidate.barcode ?: "${candidate.source}:${candidate.sourceId}"
            }
    }

    override suspend fun lookupByBarcode(
        barcode: String,
        token: UserInitiatedNetworkToken,
    ): RemoteFoodCandidate? {
        userInitiatedNetworkGuard.validate(token)
        val trimmedBarcode = barcode.trim()
        if (trimmedBarcode.isBlank() || !hasCredentials()) {
            return null
        }
        val response = api.lookupByBarcode(
            appId = appId,
            apiKey = apiKey,
            remoteUserId = remoteUserId,
            upc = trimmedBarcode,
        )
        val candidates = response.foods.orEmpty()
            .mapNotNull { food -> food.toRemoteFoodCandidate() }
        return candidates.firstOrNull { candidate ->
            candidate.barcode.equals(trimmedBarcode, ignoreCase = true)
        } ?: candidates.firstOrNull()
    }

    private fun hasCredentials(): Boolean {
        return appId.isNotBlank() && apiKey.isNotBlank()
    }

    companion object {
        private const val BASE_URL = "https://trackapi.nutritionix.com/"

        fun create(
            okHttpClient: OkHttpClient,
            userInitiatedNetworkGuard: UserInitiatedNetworkGuard,
            appId: String,
            apiKey: String,
            remoteUserId: String = "0",
        ): NutritionixRemoteFoodDataSource {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build()
            return NutritionixRemoteFoodDataSource(
                api = retrofit.create(NutritionixApi::class.java),
                userInitiatedNetworkGuard = userInitiatedNetworkGuard,
                appId = appId,
                apiKey = apiKey,
                remoteUserId = remoteUserId,
            )
        }
    }
}

internal interface NutritionixApi {
    @POST("v2/natural/nutrients")
    suspend fun naturalNutrients(
        @Header("x-app-id") appId: String,
        @Header("x-app-key") apiKey: String,
        @Header("x-remote-user-id") remoteUserId: String,
        @Body request: NutritionixNaturalRequest,
    ): NutritionixNaturalResponse

    @GET("v2/search/item")
    suspend fun lookupByBarcode(
        @Header("x-app-id") appId: String,
        @Header("x-app-key") apiKey: String,
        @Header("x-remote-user-id") remoteUserId: String,
        @Query("upc") upc: String,
    ): NutritionixItemLookupResponse
}

internal data class NutritionixNaturalRequest(
    @SerializedName("query")
    val query: String,
)

internal data class NutritionixNaturalResponse(
    @SerializedName("foods")
    val foods: List<NutritionixFoodDto>?,
)

internal data class NutritionixItemLookupResponse(
    @SerializedName("foods")
    val foods: List<NutritionixFoodDto>?,
)

internal data class NutritionixFoodDto(
    @SerializedName("food_name")
    val foodName: String?,
    @SerializedName("nix_item_name")
    val nixItemName: String?,
    @SerializedName("brand_name")
    val brandName: String?,
    @SerializedName("nix_item_id")
    val nixItemId: String?,
    @SerializedName("upc")
    val upc: String?,
    @SerializedName("serving_qty")
    val servingQty: Double?,
    @SerializedName("serving_unit")
    val servingUnit: String?,
    @SerializedName("serving_weight_grams")
    val servingWeightGrams: Double?,
    @SerializedName("nf_calories")
    val calories: Double?,
    @SerializedName("nf_protein")
    val protein: Double?,
    @SerializedName("nf_total_carbohydrate")
    val carbs: Double?,
    @SerializedName("nf_total_fat")
    val fat: Double?,
)

private fun NutritionixFoodDto.toRemoteFoodCandidate(): RemoteFoodCandidate? {
    val name = foodName.normalizedOrNull().orIfBlank(nixItemName.normalizedOrNull()) ?: return null
    val brand = brandName.normalizedOrNull()
    val barcode = upc.normalizedOrNull()
    val sourceId = nixItemId.normalizedOrNull()
        .orIfBlank(barcode)
        ?: buildNutritionixDerivedId(
            name = name,
            brand = brand,
            barcode = barcode,
        )
    val normalizedServingUnit = normalizeServingUnit(servingUnit)

    return RemoteFoodCandidate(
        source = RemoteFoodSource.NUTRITIONIX,
        sourceId = sourceId,
        barcode = barcode,
        name = name,
        brand = brand,
        caloriesKcalPer100g = per100EquivalentFromServing(
            nutrientValue = calories,
            nutrientKind = ServingNutrientKind.CALORIES,
            servingWeightGrams = servingWeightGrams,
            servingQuantity = servingQty,
            servingUnit = normalizedServingUnit,
        ),
        proteinGPer100g = per100EquivalentFromServing(
            nutrientValue = protein,
            nutrientKind = ServingNutrientKind.MACRO,
            servingWeightGrams = servingWeightGrams,
            servingQuantity = servingQty,
            servingUnit = normalizedServingUnit,
        ),
        carbsGPer100g = per100EquivalentFromServing(
            nutrientValue = carbs,
            nutrientKind = ServingNutrientKind.MACRO,
            servingWeightGrams = servingWeightGrams,
            servingQuantity = servingQty,
            servingUnit = normalizedServingUnit,
        ),
        fatGPer100g = per100EquivalentFromServing(
            nutrientValue = fat,
            nutrientKind = ServingNutrientKind.MACRO,
            servingWeightGrams = servingWeightGrams,
            servingQuantity = servingQty,
            servingUnit = normalizedServingUnit,
        ),
        servingSize = buildServingText(
            servingQuantity = servingQty,
            servingUnit = normalizedServingUnit,
            servingWeightGrams = servingWeightGrams,
        ),
    )
}

private fun String?.normalizedOrNull(): String? {
    return this?.trim()?.takeIf { it.isNotEmpty() }
}

private fun String?.orIfBlank(fallback: String?): String? {
    return if (this.isNullOrBlank()) fallback else this
}

private fun buildNutritionixDerivedId(
    name: String,
    brand: String?,
    barcode: String?,
): String {
    val normalized = listOf(name, brand.orEmpty(), barcode.orEmpty())
        .joinToString(separator = "|")
        .trim()
        .lowercase(Locale.ROOT)
    val hash = normalized.hashCode().toUInt().toString(16)
    return "nutritionix-derived-$hash"
}
