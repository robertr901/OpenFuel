package com.openfuel.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.MealEntry
import com.openfuel.app.domain.model.MealType
import com.openfuel.app.domain.repository.FoodRepository
import com.openfuel.app.domain.repository.LogRepository
import com.openfuel.app.domain.util.EntryValidation
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FoodDetailViewModel(
    private val foodRepository: FoodRepository,
    private val logRepository: LogRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val foodId: String? = savedStateHandle.get<String>(FOOD_ID_KEY)
    private val _uiState = MutableStateFlow(FoodDetailUiState())
    val uiState: StateFlow<FoodDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val food = foodId?.let { foodRepository.getFoodById(it) }
            _uiState.update { current ->
                current.copy(food = food)
            }
        }
    }

    fun toggleFavorite() {
        val food = _uiState.value.food ?: return
        viewModelScope.launch {
            try {
                val nextFavorite = !food.isFavorite
                foodRepository.setFavorite(food.id, nextFavorite)
                _uiState.update { current ->
                    current.copy(
                        food = current.food?.copy(isFavorite = nextFavorite),
                        message = if (nextFavorite) {
                            "Added to favorites."
                        } else {
                            "Removed from favorites."
                        },
                    )
                }
            } catch (_: Exception) {
                _uiState.update { current -> current.copy(message = "Could not update favorite.") }
            }
        }
    }

    fun toggleReportedIncorrect() {
        val food = _uiState.value.food ?: return
        viewModelScope.launch {
            try {
                val nextValue = !food.isReportedIncorrect
                foodRepository.setReportedIncorrect(food.id, nextValue)
                _uiState.update { current ->
                    current.copy(
                        food = current.food?.copy(isReportedIncorrect = nextValue),
                        message = if (nextValue) {
                            "Marked as incorrect on this device."
                        } else {
                            "Incorrect flag removed."
                        },
                    )
                }
            } catch (_: Exception) {
                _uiState.update { current -> current.copy(message = "Could not update report flag.") }
            }
        }
    }

    fun logFood(quantity: Double, unit: FoodUnit, mealType: MealType) {
        val food = _uiState.value.food ?: return
        if (!EntryValidation.isValidQuantity(quantity)) {
            _uiState.update { current -> current.copy(message = "Enter a valid quantity") }
            return
        }
        viewModelScope.launch {
            try {
                logRepository.logMealEntry(
                    MealEntry(
                        id = UUID.randomUUID().toString(),
                        timestamp = Instant.now(),
                        mealType = mealType,
                        foodItemId = food.id,
                        quantity = quantity,
                        unit = unit,
                    ),
                )
                _uiState.update { current -> current.copy(message = "Food logged.") }
            } catch (_: Exception) {
                _uiState.update { current -> current.copy(message = "Could not log food. Please try again.") }
            }
        }
    }

    fun consumeMessage() {
        _uiState.update { current -> current.copy(message = null) }
    }

    private companion object {
        private const val FOOD_ID_KEY = "foodId"
    }
}

data class FoodDetailUiState(
    val food: FoodItem? = null,
    val message: String? = null,
)
