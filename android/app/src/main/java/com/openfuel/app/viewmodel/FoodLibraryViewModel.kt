package com.openfuel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfuel.app.domain.diagnostics.NoOpPerformanceTraceLogger
import com.openfuel.app.domain.diagnostics.PerformanceTraceLogger
import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.MealEntry
import com.openfuel.app.domain.model.MealType
import com.openfuel.app.domain.repository.FoodRepository
import com.openfuel.app.domain.repository.LogRepository
import com.openfuel.app.domain.util.EntryValidation
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class FoodLibraryViewModel(
    private val foodRepository: FoodRepository,
    private val logRepository: LogRepository,
    private val nowInstant: () -> Instant = { Instant.now() },
    private val nowEpochMillis: () -> Long = { System.currentTimeMillis() },
    private val performanceTraceLogger: PerformanceTraceLogger = NoOpPerformanceTraceLogger,
) : ViewModel() {
    private companion object {
        private const val SEARCH_LIMIT = 20
    }

    private val searchQuery = MutableStateFlow("")
    private val message = MutableStateFlow<String?>(null)
    private val screenOpenedAtMs = nowEpochMillis()

    private val debouncedSearchQuery: StateFlow<String> = searchQuery
        .map { query -> query.trim() }
        .debounce(300)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = "",
        )

    private val localResults: StateFlow<List<FoodItem>> = debouncedSearchQuery
        .flatMapLatest { query -> localResultsFlow(query) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val recentFoods: StateFlow<List<FoodItem>> = foodRepository.recentLoggedFoods(SEARCH_LIMIT)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val favoriteFoods: StateFlow<List<FoodItem>> = foodRepository.favoriteFoods(SEARCH_LIMIT)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val uiState: StateFlow<FoodLibraryUiState> = combine(
        debouncedSearchQuery,
        localResults,
        recentFoods,
        favoriteFoods,
        message,
    ) { query, results, recents, favorites, currentMessage ->
        FoodLibraryUiState(
            searchQuery = query,
            foods = results,
            localResults = results,
            recentFoods = recents,
            favoriteFoods = favorites,
            message = currentMessage,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FoodLibraryUiState(
                searchQuery = "",
                foods = emptyList(),
                localResults = emptyList(),
                recentFoods = emptyList(),
                favoriteFoods = emptyList(),
                message = null,
            ),
        )

    init {
        viewModelScope.launch {
            uiState.first()
            recordPerformanceTrace(
                section = "foods.open",
                startedAtMs = screenOpenedAtMs,
                result = "ready",
            )
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery.update { query }
    }

    fun logFood(
        foodId: String,
        mealType: MealType,
        quantity: Double = 1.0,
        unit: FoodUnit = FoodUnit.SERVING,
    ) {
        val startedAtMs = nowEpochMillis()
        if (!EntryValidation.isValidQuantity(quantity)) {
            message.update { "Enter a valid quantity greater than 0." }
            recordPerformanceTrace(
                section = "foods.log_food",
                startedAtMs = startedAtMs,
                result = "invalid_quantity",
            )
            return
        }
        viewModelScope.launch {
            try {
                logRepository.logMealEntry(
                    MealEntry(
                        id = UUID.randomUUID().toString(),
                        timestamp = nowInstant(),
                        mealType = mealType,
                        foodItemId = foodId,
                        quantity = quantity,
                        unit = unit,
                    ),
                )
                message.update { "Food logged." }
                recordPerformanceTrace(
                    section = "foods.log_food",
                    startedAtMs = startedAtMs,
                    result = "success",
                )
            } catch (_: Exception) {
                message.update { "Could not log food. Please try again." }
                recordPerformanceTrace(
                    section = "foods.log_food",
                    startedAtMs = startedAtMs,
                    result = "error",
                )
            }
        }
    }

    fun consumeMessage() {
        message.update { null }
    }

    private fun localResultsFlow(query: String): Flow<List<FoodItem>> {
        if (query.isBlank()) {
            return flowOf(emptyList())
        }
        return foodRepository.allFoods(query)
    }

    private fun recordPerformanceTrace(
        section: String,
        startedAtMs: Long,
        result: String,
    ) {
        performanceTraceLogger.record(
            section = section,
            durationMs = (nowEpochMillis() - startedAtMs).coerceAtLeast(0L),
            result = result,
        )
    }
}

data class FoodLibraryUiState(
    val searchQuery: String,
    val foods: List<FoodItem>,
    val localResults: List<FoodItem> = foods,
    val recentFoods: List<FoodItem> = emptyList(),
    val favoriteFoods: List<FoodItem> = emptyList(),
    val message: String? = null,
)
