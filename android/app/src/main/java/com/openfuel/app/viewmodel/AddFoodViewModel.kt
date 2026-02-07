package com.openfuel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfuel.app.data.remote.RemoteFoodDataSource
import com.openfuel.app.data.remote.UserInitiatedNetworkGuard
import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.MealEntry
import com.openfuel.app.domain.model.MealType
import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.domain.repository.FoodRepository
import com.openfuel.app.domain.repository.LogRepository
import com.openfuel.app.domain.repository.SettingsRepository
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
    private val remoteFoodDataSource: RemoteFoodDataSource,
    private val userInitiatedNetworkGuard: UserInitiatedNetworkGuard,
) : ViewModel() {
    private val sectionLimit = 20
    private val searchQuery = MutableStateFlow("")
    private val onlineState = MutableStateFlow(OnlineSearchState())

    private val localSearchState: StateFlow<LocalSearchState> = searchQuery
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            foodRepository.searchFoods(query = query, limit = 20)
                .map { foods ->
                    LocalSearchState(
                        searchQuery = query,
                        foods = foods,
                    )
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LocalSearchState(
                searchQuery = "",
                foods = emptyList(),
            ),
        )

    private val favoriteFoodsState: StateFlow<List<FoodItem>> = foodRepository.favoriteFoods(sectionLimit)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val recentLoggedFoodsState: StateFlow<List<FoodItem>> = foodRepository.recentLoggedFoods(sectionLimit)
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
        localSearchState,
        onlineState,
        favoriteFoodsState,
        recentLoggedFoodsState,
        onlineLookupEnabledState,
    ) { local, online, favorites, recents, onlineLookupEnabled ->
        AddFoodUiState(
            searchQuery = local.searchQuery,
            foods = local.foods,
            favoriteFoods = favorites,
            recentLoggedFoods = recents,
            onlineLookupEnabled = onlineLookupEnabled,
            onlineResults = online.onlineResults,
            isOnlineSearchInProgress = online.isLoading,
            onlineErrorMessage = online.errorMessage,
            selectedOnlineFood = online.selectedOnlineFood,
            message = online.message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AddFoodUiState(
            searchQuery = "",
            foods = emptyList(),
            favoriteFoods = emptyList(),
            recentLoggedFoods = emptyList(),
            onlineLookupEnabled = true,
            onlineResults = emptyList(),
            isOnlineSearchInProgress = false,
            onlineErrorMessage = null,
            selectedOnlineFood = null,
            message = null,
        ),
    )

    fun updateSearchQuery(query: String) {
        searchQuery.update { query }
    }

    fun searchOnline() {
        if (!onlineLookupEnabledState.value) {
            onlineState.update { current ->
                current.copy(
                    isLoading = false,
                    onlineResults = emptyList(),
                    errorMessage = "Online search is turned off. Enable it in Settings to continue.",
                )
            }
            return
        }
        val query = searchQuery.value.trim()
        if (query.isBlank()) {
            onlineState.update { current ->
                current.copy(
                    isLoading = false,
                    onlineResults = emptyList(),
                    errorMessage = "Enter a search term to look up online.",
                )
            }
            return
        }

        viewModelScope.launch {
            onlineState.update { current ->
                current.copy(
                    isLoading = true,
                    errorMessage = null,
                )
            }
            try {
                val token = userInitiatedNetworkGuard.issueToken("add_food_search_online")
                val results = remoteFoodDataSource.searchByText(query, token)
                onlineState.update { current ->
                    current.copy(
                        isLoading = false,
                        onlineResults = results,
                        errorMessage = null,
                    )
                }
            } catch (_: Exception) {
                onlineState.update { current ->
                    current.copy(
                        isLoading = false,
                        onlineResults = emptyList(),
                        errorMessage = "Online search failed. Check connection and try again.",
                    )
                }
            }
        }
    }

    fun openOnlineFoodPreview(food: RemoteFoodCandidate) {
        onlineState.update { current -> current.copy(selectedOnlineFood = food) }
    }

    fun closeOnlineFoodPreview() {
        onlineState.update { current -> current.copy(selectedOnlineFood = null) }
    }

    fun saveOnlineFood(food: RemoteFoodCandidate) {
        viewModelScope.launch {
            try {
                foodRepository.upsertFood(food.toLocalFoodItem())
                onlineState.update { current ->
                    current.copy(
                        selectedOnlineFood = null,
                        message = "Saved to My Foods.",
                    )
                }
            } catch (_: Exception) {
                onlineState.update { current ->
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
            onlineState.update { current ->
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
                onlineState.update { current ->
                    current.copy(
                        selectedOnlineFood = null,
                        message = "Saved and logged.",
                    )
                }
            } catch (_: Exception) {
                onlineState.update { current ->
                    current.copy(message = "Could not save and log food. Please try again.")
                }
            }
        }
    }

    fun consumeMessage() {
        onlineState.update { current -> current.copy(message = null) }
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
            val food = FoodItem(
                id = UUID.randomUUID().toString(),
                name = name.ifBlank { "Quick Add" },
                brand = null,
                caloriesKcal = caloriesKcal,
                proteinG = proteinG,
                carbsG = carbsG,
                fatG = fatG,
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
    val searchQuery: String,
    val foods: List<FoodItem>,
    val favoriteFoods: List<FoodItem>,
    val recentLoggedFoods: List<FoodItem>,
    val onlineLookupEnabled: Boolean,
    val onlineResults: List<RemoteFoodCandidate>,
    val isOnlineSearchInProgress: Boolean,
    val onlineErrorMessage: String?,
    val selectedOnlineFood: RemoteFoodCandidate?,
    val message: String?,
)

private data class LocalSearchState(
    val searchQuery: String,
    val foods: List<FoodItem>,
)

private data class OnlineSearchState(
    val onlineResults: List<RemoteFoodCandidate> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedOnlineFood: RemoteFoodCandidate? = null,
    val message: String? = null,
)

private fun RemoteFoodCandidate.toLocalFoodItem(): FoodItem {
    return FoodItem(
        id = UUID.randomUUID().toString(),
        name = name,
        brand = brand,
        barcode = barcode,
        caloriesKcal = caloriesKcalPer100g?.coerceAtLeast(0.0) ?: 0.0,
        proteinG = proteinGPer100g?.coerceAtLeast(0.0) ?: 0.0,
        carbsG = carbsGPer100g?.coerceAtLeast(0.0) ?: 0.0,
        fatG = fatGPer100g?.coerceAtLeast(0.0) ?: 0.0,
        isFavorite = false,
        createdAt = Instant.now(),
    )
}
