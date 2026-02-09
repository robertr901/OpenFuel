package com.openfuel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfuel.app.data.remote.ProviderExecutorOnlineSearchOrchestrator
import com.openfuel.app.data.remote.UserInitiatedNetworkGuard
import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.MealEntry
import com.openfuel.app.domain.model.MealType
import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.domain.model.toLocalFoodItem
import com.openfuel.app.domain.search.OnlineProviderRun
import com.openfuel.app.domain.search.OnlineProviderRunStatus
import com.openfuel.app.domain.search.OnlineSearchOrchestrator
import com.openfuel.app.domain.search.OnlineSearchRequest
import com.openfuel.app.domain.repository.FoodRepository
import com.openfuel.app.domain.repository.LogRepository
import com.openfuel.app.domain.repository.SettingsRepository
import com.openfuel.app.domain.search.SearchSourceFilter
import com.openfuel.app.domain.search.UnifiedFoodSearchResult
import com.openfuel.app.domain.search.UnifiedSearchState
import com.openfuel.app.domain.search.applySourceFilter
import com.openfuel.app.domain.search.mergeUnifiedSearchResults
import com.openfuel.app.domain.service.FoodCatalogExecutionProvider
import com.openfuel.app.domain.service.FoodCatalogProvider
import com.openfuel.app.domain.service.FoodCatalogProviderDescriptor
import com.openfuel.app.domain.service.FoodCatalogProviderRegistry
import com.openfuel.app.domain.service.ProviderExecutor
import com.openfuel.app.domain.service.ProviderRefreshPolicy
import com.openfuel.app.domain.service.ProviderRequestType
import com.openfuel.app.domain.service.ProviderResult
import com.openfuel.app.domain.util.EntryValidation
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class AddFoodViewModel(
    private val foodRepository: FoodRepository,
    private val logRepository: LogRepository,
    settingsRepository: SettingsRepository,
    providerExecutor: ProviderExecutor,
    private val userInitiatedNetworkGuard: UserInitiatedNetworkGuard,
    private val onlineSearchOrchestrator: OnlineSearchOrchestrator = ProviderExecutorOnlineSearchOrchestrator(
        providerExecutor = providerExecutor,
        providerRegistry = EmptyFoodCatalogProviderRegistry,
    ),
) : ViewModel() {
    private companion object {
        private const val MAX_CALORIES_KCAL = 10_000.0
        private const val MAX_MACRO_GRAMS = 1_000.0
        private const val SEARCH_LIMIT = 20
    }

    private val unifiedSearchState = MutableStateFlow(UnifiedSearchState())
    private val transientState = MutableStateFlow(AddFoodTransientState())

    private val localSearchResultsState: StateFlow<List<FoodItem>> = unifiedSearchState
        .map { state -> state.query }
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            foodRepository.searchFoods(query = query, limit = SEARCH_LIMIT)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val favoriteFoodsState: StateFlow<List<FoodItem>> = foodRepository.favoriteFoods(SEARCH_LIMIT)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val recentLoggedFoodsState: StateFlow<List<FoodItem>> = foodRepository.recentLoggedFoods(SEARCH_LIMIT)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val onlineLookupEnabledState: StateFlow<Boolean> = settingsRepository.onlineLookupEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true,
        )

    val uiState: StateFlow<AddFoodUiState> = combine(
        unifiedSearchState,
        localSearchResultsState,
        favoriteFoodsState,
        recentLoggedFoodsState,
        onlineLookupEnabledState,
    ) { unified, localResults, favoriteFoods, recentFoods, onlineEnabled ->
        UnifiedSearchComposedState(
            unified = unified,
            localResults = localResults,
            favoriteFoods = favoriteFoods,
            recentFoods = recentFoods,
            onlineEnabled = onlineEnabled,
        )
    }.combine(transientState) { composed, transient ->
        val mergedResults = applySourceFilter(
            results = mergeUnifiedSearchResults(
                localResults = composed.localResults,
                onlineResults = composed.unified.onlineResults,
            ),
            sourceFilter = composed.unified.sourceFilter,
        )
        val effectiveUnifiedSearch = composed.unified.copy(
            localResults = composed.localResults,
            mergedResults = mergedResults,
            onlineEnabled = composed.onlineEnabled,
        )

        AddFoodUiState(
            unifiedSearch = effectiveUnifiedSearch,
            searchQuery = effectiveUnifiedSearch.query,
            sourceFilter = effectiveUnifiedSearch.sourceFilter,
            foods = composed.localResults,
            favoriteFoods = composed.favoriteFoods,
            recentLoggedFoods = composed.recentFoods,
            mergedResults = effectiveUnifiedSearch.mergedResults,
            onlineLookupEnabled = effectiveUnifiedSearch.onlineEnabled,
            hasSearchedOnline = effectiveUnifiedSearch.onlineHasSearched,
            onlineResults = effectiveUnifiedSearch.onlineResults,
            onlineProviderRuns = effectiveUnifiedSearch.providerRuns,
            onlineProviderResults = effectiveUnifiedSearch.providerResults,
            onlineExecutionElapsedMs = effectiveUnifiedSearch.onlineElapsedMs,
            onlineExecutionCount = effectiveUnifiedSearch.onlineExecutionCount,
            isOnlineSearchInProgress = effectiveUnifiedSearch.onlineIsLoading,
            onlineErrorMessage = effectiveUnifiedSearch.onlineError,
            selectedOnlineFood = transient.selectedOnlineFood,
            message = transient.message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AddFoodUiState(),
    )

    fun updateSearchQuery(query: String) {
        unifiedSearchState.update { current ->
            current.copy(
                query = query,
                onlineResults = emptyList(),
                onlineHasSearched = false,
                onlineIsLoading = false,
                onlineError = null,
                providerRuns = emptyList(),
                providerResults = emptyList(),
                onlineElapsedMs = 0L,
                onlineExecutionCount = 0,
            )
        }
        transientState.update { current ->
            current.copy(selectedOnlineFood = null)
        }
    }

    fun setSourceFilter(sourceFilter: SearchSourceFilter) {
        unifiedSearchState.update { current -> current.copy(sourceFilter = sourceFilter) }
    }

    fun searchOnline() {
        executeOnlineSearch(refreshPolicy = ProviderRefreshPolicy.CACHE_PREFERRED)
    }

    fun refreshOnline() {
        executeOnlineSearch(refreshPolicy = ProviderRefreshPolicy.FORCE_REFRESH)
    }

    private fun executeOnlineSearch(refreshPolicy: ProviderRefreshPolicy) {
        if (!onlineLookupEnabledState.value) {
            unifiedSearchState.update { current ->
                current.copy(
                    onlineEnabled = false,
                    onlineHasSearched = false,
                    onlineIsLoading = false,
                    onlineResults = emptyList(),
                    onlineError = "Online search is turned off. Enable it in Settings to continue.",
                    providerRuns = emptyList(),
                    providerResults = emptyList(),
                    onlineElapsedMs = 0L,
                )
            }
            return
        }

        val query = unifiedSearchState.value.query.trim()
        if (query.isBlank()) {
            unifiedSearchState.update { current ->
                current.copy(
                    onlineHasSearched = false,
                    onlineIsLoading = false,
                    onlineResults = emptyList(),
                    onlineError = "Enter a search term to look up online.",
                    providerRuns = emptyList(),
                    providerResults = emptyList(),
                    onlineElapsedMs = 0L,
                )
            }
            return
        }

        viewModelScope.launch {
            unifiedSearchState.update { current ->
                current.copy(
                    onlineHasSearched = true,
                    onlineIsLoading = true,
                    onlineResults = emptyList(),
                    onlineError = null,
                    providerRuns = emptyList(),
                    providerResults = emptyList(),
                    onlineElapsedMs = 0L,
                )
            }
            try {
                val token = userInitiatedNetworkGuard.issueToken("add_food_search_online")
                val result = onlineSearchOrchestrator.search(
                    request = OnlineSearchRequest(
                        query = query,
                        token = token,
                        onlineLookupEnabled = onlineLookupEnabledState.value,
                        refreshPolicy = refreshPolicy,
                    ),
                )
                val results = result.candidates
                val error = deriveOnlineErrorMessage(
                    providerRuns = result.providerRuns,
                    hasResults = results.isNotEmpty(),
                )
                unifiedSearchState.update { current ->
                    current.copy(
                        onlineHasSearched = true,
                        onlineIsLoading = false,
                        onlineResults = results,
                        onlineError = error,
                        providerRuns = result.providerRuns,
                        providerResults = result.providerResults,
                        onlineElapsedMs = result.overallDurationMs,
                        onlineExecutionCount = current.onlineExecutionCount + 1,
                    )
                }
            } catch (_: Exception) {
                unifiedSearchState.update { current ->
                    current.copy(
                        onlineHasSearched = true,
                        onlineIsLoading = false,
                        onlineResults = emptyList(),
                        onlineError = "Online search failed. Check connection and try again.",
                        providerRuns = emptyList(),
                        providerResults = emptyList(),
                        onlineElapsedMs = 0L,
                        onlineExecutionCount = current.onlineExecutionCount + 1,
                    )
                }
            }
        }
    }

    fun openOnlineFoodPreview(food: RemoteFoodCandidate) {
        transientState.update { current -> current.copy(selectedOnlineFood = food) }
    }

    fun closeOnlineFoodPreview() {
        transientState.update { current -> current.copy(selectedOnlineFood = null) }
    }

    fun saveOnlineFood(food: RemoteFoodCandidate) {
        viewModelScope.launch {
            try {
                foodRepository.upsertFood(food.toLocalFoodItem())
                transientState.update { current ->
                    current.copy(
                        selectedOnlineFood = null,
                        message = "Saved to My Foods.",
                    )
                }
            } catch (_: Exception) {
                transientState.update { current ->
                    current.copy(message = "Could not save food. Please try again.")
                }
            }
        }
    }

    fun saveAndLogOnlineFood(
        food: RemoteFoodCandidate,
        mealType: MealType,
        quantity: Double,
        unit: FoodUnit,
    ) {
        if (!EntryValidation.isValidQuantity(quantity)) {
            transientState.update { current ->
                current.copy(message = "Enter a valid quantity greater than 0.")
            }
            return
        }
        viewModelScope.launch {
            try {
                val localFood = food.toLocalFoodItem()
                foodRepository.upsertFood(localFood)
                logRepository.logMealEntry(
                    MealEntry(
                        id = UUID.randomUUID().toString(),
                        timestamp = Instant.now(),
                        mealType = mealType,
                        foodItemId = localFood.id,
                        quantity = quantity,
                        unit = unit,
                    ),
                )
                transientState.update { current ->
                    current.copy(
                        selectedOnlineFood = null,
                        message = "Saved and logged.",
                    )
                }
            } catch (_: Exception) {
                transientState.update { current ->
                    current.copy(message = "Could not save and log food. Please try again.")
                }
            }
        }
    }

    fun consumeMessage() {
        transientState.update { current -> current.copy(message = null) }
    }

    fun logFood(foodId: String, mealType: MealType, quantity: Double, unit: FoodUnit) {
        viewModelScope.launch {
            val entry = MealEntry(
                id = UUID.randomUUID().toString(),
                timestamp = Instant.now(),
                mealType = mealType,
                foodItemId = foodId,
                quantity = quantity,
                unit = unit,
            )
            logRepository.logMealEntry(entry)
        }
    }

    fun quickAdd(
        name: String,
        caloriesKcal: Double,
        proteinG: Double,
        carbsG: Double,
        fatG: Double,
        mealType: MealType,
    ) {
        viewModelScope.launch {
            val safeCalories = caloriesKcal.coerceIn(0.0, MAX_CALORIES_KCAL)
            val safeProtein = proteinG.coerceIn(0.0, MAX_MACRO_GRAMS)
            val safeCarbs = carbsG.coerceIn(0.0, MAX_MACRO_GRAMS)
            val safeFat = fatG.coerceIn(0.0, MAX_MACRO_GRAMS)
            val food = FoodItem(
                id = UUID.randomUUID().toString(),
                name = name.ifBlank { "Quick Add" },
                brand = null,
                caloriesKcal = safeCalories,
                proteinG = safeProtein,
                carbsG = safeCarbs,
                fatG = safeFat,
                createdAt = Instant.now(),
            )
            foodRepository.upsertFood(food)
            val entry = MealEntry(
                id = UUID.randomUUID().toString(),
                timestamp = Instant.now(),
                mealType = mealType,
                foodItemId = food.id,
                quantity = 1.0,
                unit = FoodUnit.SERVING,
            )
            logRepository.logMealEntry(entry)
        }
    }
}

data class AddFoodUiState(
    val unifiedSearch: UnifiedSearchState = UnifiedSearchState(),
    val searchQuery: String = "",
    val sourceFilter: SearchSourceFilter = SearchSourceFilter.ALL,
    val foods: List<FoodItem> = emptyList(),
    val favoriteFoods: List<FoodItem> = emptyList(),
    val recentLoggedFoods: List<FoodItem> = emptyList(),
    val mergedResults: List<UnifiedFoodSearchResult> = emptyList(),
    val onlineLookupEnabled: Boolean = true,
    val hasSearchedOnline: Boolean = false,
    val onlineResults: List<RemoteFoodCandidate> = emptyList(),
    val onlineProviderRuns: List<OnlineProviderRun> = emptyList(),
    val onlineProviderResults: List<ProviderResult> = emptyList(),
    val onlineExecutionElapsedMs: Long = 0L,
    val onlineExecutionCount: Int = 0,
    val isOnlineSearchInProgress: Boolean = false,
    val onlineErrorMessage: String? = null,
    val selectedOnlineFood: RemoteFoodCandidate? = null,
    val message: String? = null,
)

private data class AddFoodTransientState(
    val selectedOnlineFood: RemoteFoodCandidate? = null,
    val message: String? = null,
)

private data class UnifiedSearchComposedState(
    val unified: UnifiedSearchState,
    val localResults: List<FoodItem>,
    val favoriteFoods: List<FoodItem>,
    val recentFoods: List<FoodItem>,
    val onlineEnabled: Boolean,
)

private fun deriveOnlineErrorMessage(
    providerRuns: List<OnlineProviderRun>,
    hasResults: Boolean,
): String? {
    if (providerRuns.isEmpty()) {
        return null
    }
    if (hasResults) {
        return null
    }
    val missingConfigRuns = providerRuns.filter { run ->
        run.status == OnlineProviderRunStatus.SKIPPED_MISSING_CONFIG
    }
    val failedRuns = providerRuns.filter { run ->
        run.status == OnlineProviderRunStatus.FAILED
    }

    if (missingConfigRuns.isNotEmpty() && failedRuns.isEmpty()) {
        return if (missingConfigRuns.size == 1) {
            "Source needs setup. See statuses below."
        } else {
            "Some sources need setup. See statuses below."
        }
    }

    if (failedRuns.isNotEmpty()) {
        return if (failedRuns.size == 1 && missingConfigRuns.isEmpty()) {
            failedRuns.single().message ?: "A source failed. See statuses below."
        } else {
            "Some sources failed. See statuses below."
        }
    }

    return null
}

private object EmptyFoodCatalogProviderRegistry : FoodCatalogProviderRegistry {
    override fun providersFor(
        requestType: ProviderRequestType,
        onlineLookupEnabled: Boolean,
    ): List<FoodCatalogExecutionProvider> {
        return emptyList()
    }

    override fun primaryTextSearchProvider(): FoodCatalogProvider {
        error("No providers configured in EmptyFoodCatalogProviderRegistry.")
    }

    override fun providerDiagnostics(onlineLookupEnabled: Boolean): List<FoodCatalogProviderDescriptor> {
        return emptyList()
    }
}
