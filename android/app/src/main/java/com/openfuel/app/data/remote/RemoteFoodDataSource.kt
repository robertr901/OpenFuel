package com.openfuel.app.data.remote

import com.google.gson.annotations.SerializedName
import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.domain.model.RemoteFoodSource
import java.io.IOException
import java.net.SocketTimeoutException
import okhttp3.OkHttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.HttpException
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.Locale

private const val OPEN_FOOD_FACTS_SEARCH_FIELDS =
    "code,_id,id,product_name,product_name_en,generic_name,brands,serving_size,nutriments"
private const val OPEN_FOOD_FACTS_RETRY_BACKOFF_MS = 200L

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

class OpenFoodFactsRemoteFoodDataSource internal constructor(
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
        return executeGetWithSingleRetry {
            api.searchFoods(
                query = trimmedQuery,
                pageSize = pageSize,
            )
        }.products.orEmpty()
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
        val response = executeGetWithSingleRetry {
            api.lookupByBarcode(trimmedBarcode)
        }
        if (response.status != 1) {
            return null
        }
        return response.product?.toRemoteFoodCandidate()
    }

    private suspend fun <T> executeGetWithSingleRetry(
        block: suspend () -> T,
    ): T {
        return try {
            block()
        } catch (throwable: Throwable) {
            if (throwable is CancellationException || !throwable.isRetryableGetFailure()) {
                throw throwable
            }
            delay(OPEN_FOOD_FACTS_RETRY_BACKOFF_MS)
            block()
        }
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

private fun Throwable.isRetryableGetFailure(): Boolean {
    val root = rootCause()
    return when (root) {
        is SocketTimeoutException -> true
        is IOException -> true
        is HttpException -> root.code() in setOf(408, 425, 429, 500, 502, 503, 504)
        else -> false
    }
}

private fun Throwable.rootCause(): Throwable {
    var current: Throwable = this
    while (current.cause != null && current.cause !== current) {
        current = current.cause!!
    }
    return current
}

internal interface OpenFoodFactsApi {
    @GET("cgi/search.pl")
    suspend fun searchFoods(
        @Query("search_terms") query: String,
        @Query("search_simple") searchSimple: Int = 1,
        @Query("action") action: String = "process",
        @Query("json") json: Int = 1,
        @Query("fields") fields: String = OPEN_FOOD_FACTS_SEARCH_FIELDS,
        @Query("page_size") pageSize: Int,
    ): OpenFoodFactsSearchResponse

    @GET("api/v2/product/{barcode}")
    suspend fun lookupByBarcode(
        @Path("barcode") barcode: String,
    ): OpenFoodFactsLookupResponse
}

internal data class OpenFoodFactsSearchResponse(
    @SerializedName("products")
    val products: List<OpenFoodFactsProductDto>?,
)

internal data class OpenFoodFactsLookupResponse(
    @SerializedName("status")
    val status: Int,
    @SerializedName("product")
    val product: OpenFoodFactsProductDto?,
)

internal data class OpenFoodFactsProductDto(
    @SerializedName("code")
    val code: String?,
    @SerializedName(value = "_id", alternate = ["id"])
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

internal data class OpenFoodFactsNutrimentsDto(
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
    val normalizedProductName = productName.normalizedOrNull()
    val normalizedProductNameEn = productNameEn.normalizedOrNull()
    val normalizedGenericName = genericName.normalizedOrNull()
    val normalizedBrand = brands.normalizedOrNull()
    val normalizedServingSize = servingSize.normalizedOrNull()
    val normalizedCode = code.normalizedOrNull()
    val normalizedId = id.normalizedOrNull()

    val resolvedName = normalizedProductName
        .orIfBlank(normalizedProductNameEn)
        .orIfBlank(normalizedGenericName)
    if (resolvedName.isNullOrBlank()) {
        return null
    }
    val resolvedCode = normalizedCode
        .orIfBlank(normalizedId)
        ?: buildDerivedSourceId(
            name = resolvedName,
            brand = normalizedBrand,
            servingSize = normalizedServingSize,
        )
    return RemoteFoodCandidate(
        source = RemoteFoodSource.OPEN_FOOD_FACTS,
        sourceId = resolvedCode,
        barcode = normalizedCode,
        name = resolvedName,
        brand = normalizedBrand,
        caloriesKcalPer100g = nutriments?.energyKcal100g.sanitizeNutrient()
            ?: nutriments?.energyKcal.sanitizeNutrient(),
        proteinGPer100g = nutriments?.proteins100g.sanitizeNutrient()
            ?: nutriments?.proteins.sanitizeNutrient(),
        carbsGPer100g = nutriments?.carbohydrates100g.sanitizeNutrient()
            ?: nutriments?.carbohydrates.sanitizeNutrient(),
        fatGPer100g = nutriments?.fat100g.sanitizeNutrient()
            ?: nutriments?.fat.sanitizeNutrient(),
        servingSize = normalizedServingSize,
    )
}

private fun String?.orIfBlank(fallback: String?): String? {
    return if (this.isNullOrBlank()) fallback else this
}

private fun String?.normalizedOrNull(): String? {
    return this?.trim()?.takeIf { it.isNotEmpty() }
}

private fun Double?.sanitizeNutrient(): Double? {
    val value = this ?: return null
    return value.takeIf { it.isFinite() && it >= 0.0 }
}

private fun buildDerivedSourceId(
    name: String,
    brand: String?,
    servingSize: String?,
): String {
    val normalized = listOf(name, brand.orEmpty(), servingSize.orEmpty())
        .joinToString(separator = "|")
        .trim()
        .lowercase(Locale.ROOT)
    val hash = normalized.hashCode().toUInt().toString(16)
    return "derived-$hash"
}
