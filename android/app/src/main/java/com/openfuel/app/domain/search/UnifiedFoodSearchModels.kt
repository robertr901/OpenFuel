package com.openfuel.app.domain.search

import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.domain.service.ProviderResult
import java.util.Locale
import kotlin.math.abs

enum class SearchSourceFilter {
    ALL,
    LOCAL_ONLY,
    ONLINE_ONLY,
}

data class UnifiedSearchState(
    val query: String = "",
    val sourceFilter: SearchSourceFilter = SearchSourceFilter.ALL,
    val localResults: List<FoodItem> = emptyList(),
    val onlineResults: List<RemoteFoodCandidate> = emptyList(),
    val mergedResults: List<UnifiedFoodSearchResult> = emptyList(),
    val onlineEnabled: Boolean = true,
    val onlineHasSearched: Boolean = false,
    val onlineIsLoading: Boolean = false,
    val onlineError: String? = null,
    val providerRuns: List<OnlineProviderRun> = emptyList(),
    val providerResults: List<ProviderResult> = emptyList(),
    val onlineElapsedMs: Long = 0L,
    val onlineExecutionCount: Int = 0,
)

sealed class UnifiedFoodSearchResult {
    abstract val stableId: String

    data class LocalResult(
        val food: FoodItem,
    ) : UnifiedFoodSearchResult() {
        override val stableId: String = "local:${food.id}"
    }

    data class OnlineResult(
        val candidate: RemoteFoodCandidate,
        val isSavedLocally: Boolean,
        val provenance: String = candidate.providerKey
            ?: candidate.source.name.lowercase(Locale.ROOT),
    ) : UnifiedFoodSearchResult() {
        override val stableId: String = "online:$provenance:${candidate.source}:${candidate.sourceId}"
    }
}

fun mergeUnifiedSearchResults(
    localResults: List<FoodItem>,
    onlineResults: List<RemoteFoodCandidate>,
): List<UnifiedFoodSearchResult> {
    val locals = localResults.map { food ->
        UnifiedFoodSearchResult.LocalResult(food = food)
    }
    if (onlineResults.isEmpty()) {
        return locals
    }

    val localBarcodes = localResults.mapNotNull { food ->
        food.barcode?.trim()?.takeIf { barcode -> barcode.isNotEmpty() }
    }.toSet()

    val localNameBrandCalories = localResults.map { food ->
        Triple(
            normalizeText(food.name),
            normalizeText(food.brand.orEmpty()),
            food.caloriesKcal,
        )
    }

    val online = onlineResults.mapNotNull { candidate ->
        val normalizedBarcode = candidate.barcode?.trim()?.takeIf { barcode -> barcode.isNotEmpty() }
        val duplicateByBarcode = normalizedBarcode != null && normalizedBarcode in localBarcodes
        val duplicateByNameBrandCalories = localNameBrandCalories.any { (name, brand, calories) ->
            val candidateCalories = candidate.caloriesKcalPer100g ?: return@any false
            name == normalizeText(candidate.name) &&
                brand == normalizeText(candidate.brand.orEmpty()) &&
                abs(calories - candidateCalories) <= CALORIE_DUPLICATE_TOLERANCE_KCAL
        }

        if (duplicateByBarcode || duplicateByNameBrandCalories) {
            null
        } else {
            UnifiedFoodSearchResult.OnlineResult(
                candidate = candidate,
                isSavedLocally = false,
            )
        }
    }

    return locals + online
}

fun applySourceFilter(
    results: List<UnifiedFoodSearchResult>,
    sourceFilter: SearchSourceFilter,
): List<UnifiedFoodSearchResult> {
    return when (sourceFilter) {
        SearchSourceFilter.ALL -> results
        SearchSourceFilter.LOCAL_ONLY -> results.filterIsInstance<UnifiedFoodSearchResult.LocalResult>()
        SearchSourceFilter.ONLINE_ONLY -> results.filterIsInstance<UnifiedFoodSearchResult.OnlineResult>()
    }
}

private const val CALORIE_DUPLICATE_TOLERANCE_KCAL = 5.0

private fun normalizeText(value: String): String {
    return value.trim().lowercase().replace("\\s+".toRegex(), " ")
}
