package com.openfuel.app.data.remote

import com.google.gson.annotations.SerializedName
import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.domain.model.RemoteFoodSource
import java.util.Locale
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface UsdaFoodDataSource {
    suspend fun searchByText(
        query: String,
        token: UserInitiatedNetworkToken,
    ): List<RemoteFoodCandidate>

    suspend fun lookupByBarcode(
        barcode: String,
        token: UserInitiatedNetworkToken,
    ): RemoteFoodCandidate?
}

class UsdaFoodDataCentralDataSource internal constructor(
    private val api: UsdaFoodDataCentralApi,
    private val userInitiatedNetworkGuard: UserInitiatedNetworkGuard,
    private val apiKey: String,
    private val pageSize: Int = 20,
) : UsdaFoodDataSource {
    override suspend fun searchByText(
        query: String,
        token: UserInitiatedNetworkToken,
    ): List<RemoteFoodCandidate> {
        userInitiatedNetworkGuard.validate(token)
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank() || apiKey.isBlank()) {
            return emptyList()
        }
        val response = api.searchFoods(
            apiKey = apiKey,
            request = UsdaFoodSearchRequest(
                query = trimmedQuery,
                pageSize = pageSize,
            ),
        )
        return response.foods.orEmpty()
            .mapNotNull { it.toRemoteFoodCandidate() }
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
        if (trimmedBarcode.isBlank() || apiKey.isBlank()) {
            return null
        }
        val response = api.searchFoods(
            apiKey = apiKey,
            request = UsdaFoodSearchRequest(
                query = trimmedBarcode,
                pageSize = pageSize.coerceAtMost(10),
            ),
        )
        val candidates = response.foods.orEmpty()
            .mapNotNull { it.toRemoteFoodCandidate() }
        return candidates.firstOrNull { candidate ->
            candidate.barcode.equals(trimmedBarcode, ignoreCase = true)
        } ?: candidates.firstOrNull()
    }

    companion object {
        private const val BASE_URL = "https://api.nal.usda.gov/"

        fun create(
            okHttpClient: OkHttpClient,
            userInitiatedNetworkGuard: UserInitiatedNetworkGuard,
            apiKey: String,
            pageSize: Int = 20,
        ): UsdaFoodDataCentralDataSource {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build()
            return UsdaFoodDataCentralDataSource(
                api = retrofit.create(UsdaFoodDataCentralApi::class.java),
                userInitiatedNetworkGuard = userInitiatedNetworkGuard,
                apiKey = apiKey,
                pageSize = pageSize,
            )
        }
    }
}

internal interface UsdaFoodDataCentralApi {
    @POST("fdc/v1/foods/search")
    suspend fun searchFoods(
        @Query("api_key") apiKey: String,
        @Body request: UsdaFoodSearchRequest,
    ): UsdaFoodSearchResponse
}

internal data class UsdaFoodSearchRequest(
    @SerializedName("query")
    val query: String,
    @SerializedName("pageSize")
    val pageSize: Int,
)

internal data class UsdaFoodSearchResponse(
    @SerializedName("foods")
    val foods: List<UsdaFoodDto>?,
)

internal data class UsdaFoodDto(
    @SerializedName("fdcId")
    val fdcId: Long?,
    @SerializedName("description")
    val description: String?,
    @SerializedName("brandOwner")
    val brandOwner: String?,
    @SerializedName("brandName")
    val brandName: String?,
    @SerializedName("gtinUpc")
    val gtinUpc: String?,
    @SerializedName("servingSize")
    val servingSize: Double?,
    @SerializedName("servingSizeUnit")
    val servingSizeUnit: String?,
    @SerializedName("foodNutrients")
    val foodNutrients: List<UsdaFoodNutrientDto>?,
)

internal data class UsdaFoodNutrientDto(
    @SerializedName("nutrientNumber")
    val nutrientNumber: String?,
    @SerializedName("nutrientName")
    val nutrientName: String?,
    @SerializedName(value = "value", alternate = ["amount"])
    val value: Double?,
)

private data class UsdaNutrients(
    val caloriesKcal: Double?,
    val proteinG: Double?,
    val carbsG: Double?,
    val fatG: Double?,
)

private fun UsdaFoodDto.toRemoteFoodCandidate(): RemoteFoodCandidate? {
    val name = description.normalizedOrNull() ?: return null
    val brand = brandOwner.normalizedOrNull().orIfBlank(brandName.normalizedOrNull())
    val barcode = gtinUpc.normalizedOrNull()
    val id = fdcId?.toString()?.trim().takeIf { !it.isNullOrEmpty() } ?: buildUsdaDerivedId(
        name = name,
        brand = brand,
        barcode = barcode,
    )
    val servingUnit = servingSizeUnit.normalizedOrNull()
    val servingText = buildServingSizeText(
        servingSize = servingSize,
        servingUnit = servingUnit,
    )
    val nutrients = foodNutrients.toUsdaNutrients(
        servingSize = servingSize,
        servingUnit = servingUnit,
    )
    return RemoteFoodCandidate(
        source = RemoteFoodSource.USDA_FOODDATA_CENTRAL,
        sourceId = id,
        barcode = barcode,
        name = name,
        brand = brand,
        caloriesKcalPer100g = nutrients.caloriesKcal,
        proteinGPer100g = nutrients.proteinG,
        carbsGPer100g = nutrients.carbsG,
        fatGPer100g = nutrients.fatG,
        servingSize = servingText,
    )
}

private fun List<UsdaFoodNutrientDto>?.toUsdaNutrients(
    servingSize: Double?,
    servingUnit: String?,
): UsdaNutrients {
    val nutrients = this.orEmpty()
    val calories = nutrients.firstNutrientValue(
        nutrientNumber = "1008",
        nutrientNameFallback = "energy",
    ).per100Equivalent(servingSize = servingSize, servingUnit = servingUnit)
    val protein = nutrients.firstNutrientValue(
        nutrientNumber = "1003",
        nutrientNameFallback = "protein",
    ).per100Equivalent(servingSize = servingSize, servingUnit = servingUnit)
    val carbs = nutrients.firstNutrientValue(
        nutrientNumber = "1005",
        nutrientNameFallback = "carbohydrate",
    ).per100Equivalent(servingSize = servingSize, servingUnit = servingUnit)
    val fat = nutrients.firstNutrientValue(
        nutrientNumber = "1004",
        nutrientNameFallback = "fat",
    ).per100Equivalent(servingSize = servingSize, servingUnit = servingUnit)
    return UsdaNutrients(
        caloriesKcal = calories,
        proteinG = protein,
        carbsG = carbs,
        fatG = fat,
    )
}

private fun List<UsdaFoodNutrientDto>.firstNutrientValue(
    nutrientNumber: String,
    nutrientNameFallback: String,
): Double? {
    val exact = firstOrNull { nutrient ->
        nutrient.nutrientNumber.normalizedOrNull() == nutrientNumber
    }?.value.sanitizeNutrient()
    if (exact != null) {
        return exact
    }
    return firstOrNull { nutrient ->
        nutrient.nutrientName.normalizedOrNull()
            ?.lowercase(Locale.ROOT)
            ?.contains(nutrientNameFallback.lowercase(Locale.ROOT)) == true
    }?.value.sanitizeNutrient()
}

private fun Double?.per100Equivalent(
    servingSize: Double?,
    servingUnit: String?,
): Double? {
    val sanitized = sanitizeNutrient() ?: return null
    val normalizedUnit = servingUnit
        ?.trim()
        ?.lowercase(Locale.ROOT)
        ?.replace("\\.".toRegex(), "")
        ?: return sanitized
    val size = servingSize?.takeIf { it.isFinite() && it > 0.0 } ?: return sanitized
    return when (normalizedUnit) {
        "g", "gram", "grams", "ml", "milliliter", "milliliters", "millilitre", "millilitres" -> {
            (sanitized * (100.0 / size)).sanitizeNutrient()
        }

        else -> sanitized
    }
}

private fun buildServingSizeText(
    servingSize: Double?,
    servingUnit: String?,
): String? {
    val unit = servingUnit.normalizedOrNull()
    val size = servingSize?.takeIf { it.isFinite() && it > 0.0 }
    return when {
        size != null && unit != null -> "${size.trimmedForDisplay()} $unit"
        size != null -> size.trimmedForDisplay()
        else -> unit
    }
}

private fun Double.trimmedForDisplay(): String {
    return if (this % 1.0 == 0.0) {
        this.toLong().toString()
    } else {
        this.toString()
    }
}

private fun String?.normalizedOrNull(): String? {
    return this?.trim()?.takeIf { it.isNotEmpty() }
}

private fun String?.orIfBlank(fallback: String?): String? {
    return if (this.isNullOrBlank()) fallback else this
}

private fun Double?.sanitizeNutrient(): Double? {
    val value = this ?: return null
    return value.takeIf { it.isFinite() && it >= 0.0 }
}

private fun buildUsdaDerivedId(
    name: String,
    brand: String?,
    barcode: String?,
): String {
    val normalized = listOf(name, brand.orEmpty(), barcode.orEmpty())
        .joinToString(separator = "|")
        .trim()
        .lowercase(Locale.ROOT)
    val hash = normalized.hashCode().toUInt().toString(16)
    return "usda-derived-$hash"
}
