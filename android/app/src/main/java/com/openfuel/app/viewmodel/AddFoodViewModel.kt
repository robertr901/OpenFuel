package com.openfuel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.MealEntry
import com.openfuel.app.domain.model.MealType
import com.openfuel.app.domain.repository.FoodRepository
import com.openfuel.app.domain.repository.LogRepository
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<AddFoodUiState> = searchQuery
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            foodRepository.searchFoods(query = query, limit = 20)
                .map { foods ->
                    AddFoodUiState(
                        searchQuery = query,
                        foods = foods,
                    )
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AddFoodUiState(
                searchQuery = "",
                foods = emptyList(),
            ),
        )

    fun updateSearchQuery(query: String) {
        searchQuery.update { query }
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
)
