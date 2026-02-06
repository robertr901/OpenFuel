package com.openfuel.app.data.remote

import com.google.gson.annotations.SerializedName
import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.domain.model.RemoteFoodSource
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RemoteFoodDataSource {
    suspend fun searchByText(
        query: String,
        token: UserInitiatedNetworkToken,
    ): List<RemoteFoodCandidate>

    suspend fun lookupByBarcode(
        barcode: String,
        token: UserInitiatedNetworkToken,
    ): RemoteFoodCandidate?
}

class OpenFoodFactsRemoteFoodDataSource private constructor(
    private val api: OpenFoodFactsApi,
    private val userInitiatedNetworkGuard: UserInitiatedNetworkGuard,
    private val pageSize: Int = 20,
) : RemoteFoodDataSource {
    override suspend fun searchByText(
        query: String,
        token: UserInitiatedNetworkToken,
    ): List<RemoteFoodCandidate> {
        userInitiatedNetworkGuard.validate(token)
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            return emptyList()
        }
        return api.searchFoods(
            query = trimmedQuery,
            pageSize = pageSize,
        ).products.orEmpty()
            .mapNotNull { product -> product.toRemoteFoodCandidate() }
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
        if (trimmedBarcode.isBlank()) {
            return null
        }
        val response = api.lookupByBarcode(trimmedBarcode)
        if (response.status != 1) {
            return null
        }
        return response.product?.toRemoteFoodCandidate()
    }

    companion object {
        private const val BASE_URL = "https://world.openfoodfacts.org/"

        fun create(
            okHttpClient: OkHttpClient,
            userInitiatedNetworkGuard: UserInitiatedNetworkGuard,
            pageSize: Int = 20,
        ): OpenFoodFactsRemoteFoodDataSource {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build()
            return OpenFoodFactsRemoteFoodDataSource(
                api = retrofit.create(OpenFoodFactsApi::class.java),
                userInitiatedNetworkGuard = userInitiatedNetworkGuard,
                pageSize = pageSize,
            )
        }
    }
}

private interface OpenFoodFactsApi {
    @GET("cgi/search.pl")
    suspend fun searchFoods(
        @Query("search_terms") query: String,
        @Query("search_simple") searchSimple: Int = 1,
        @Query("action") action: String = "process",
        @Query("json") json: Int = 1,
        @Query("page_size") pageSize: Int,
    ): OpenFoodFactsSearchResponse

    @GET("api/v2/product/{barcode}")
    suspend fun lookupByBarcode(
        @Path("barcode") barcode: String,
    ): OpenFoodFactsLookupResponse
}

private data class OpenFoodFactsSearchResponse(
    @SerializedName("products")
    val products: List<OpenFoodFactsProductDto>?,
)

private data class OpenFoodFactsLookupResponse(
    @SerializedName("status")
    val status: Int,
    @SerializedName("product")
    val product: OpenFoodFactsProductDto?,
)

private data class OpenFoodFactsProductDto(
    @SerializedName("code")
    val code: String?,
    @SerializedName("_id")
    val id: String?,
    @SerializedName("product_name")
    val productName: String?,
    @SerializedName("product_name_en")
    val productNameEn: String?,
    @SerializedName("generic_name")
    val genericName: String?,
    @SerializedName("brands")
    val brands: String?,
    @SerializedName("serving_size")
    val servingSize: String?,
    @SerializedName("nutriments")
    val nutriments: OpenFoodFactsNutrimentsDto?,
)

private data class OpenFoodFactsNutrimentsDto(
    @SerializedName("energy-kcal_100g")
    val energyKcal100g: Double?,
    @SerializedName("energy-kcal")
    val energyKcal: Double?,
    @SerializedName("proteins_100g")
    val proteins100g: Double?,
    @SerializedName("proteins")
    val proteins: Double?,
    @SerializedName("carbohydrates_100g")
    val carbohydrates100g: Double?,
    @SerializedName("carbohydrates")
    val carbohydrates: Double?,
    @SerializedName("fat_100g")
    val fat100g: Double?,
    @SerializedName("fat")
    val fat: Double?,
)

private fun OpenFoodFactsProductDto.toRemoteFoodCandidate(): RemoteFoodCandidate? {
    val resolvedName = productName.orIfBlank(productNameEn).orIfBlank(genericName)
    val resolvedCode = code.orIfBlank(id)
    if (resolvedName.isNullOrBlank() || resolvedCode.isNullOrBlank()) {
        return null
    }
    return RemoteFoodCandidate(
        source = RemoteFoodSource.OPEN_FOOD_FACTS,
        sourceId = resolvedCode,
        barcode = code?.takeIf { it.isNotBlank() },
        name = resolvedName,
        brand = brands?.takeIf { it.isNotBlank() },
        caloriesKcalPer100g = nutriments?.energyKcal100g ?: nutriments?.energyKcal,
        proteinGPer100g = nutriments?.proteins100g ?: nutriments?.proteins,
        carbsGPer100g = nutriments?.carbohydrates100g ?: nutriments?.carbohydrates,
        fatGPer100g = nutriments?.fat100g ?: nutriments?.fat,
        servingSize = servingSize?.takeIf { it.isNotBlank() },
    )
}

private fun String?.orIfBlank(fallback: String?): String? {
    return if (this.isNullOrBlank()) fallback else this
}
