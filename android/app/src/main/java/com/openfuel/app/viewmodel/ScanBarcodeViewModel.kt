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
import com.openfuel.app.domain.util.EntryValidation
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScanBarcodeViewModel(
    private val remoteFoodDataSource: RemoteFoodDataSource,
    private val userInitiatedNetworkGuard: UserInitiatedNetworkGuard,
    private val foodRepository: FoodRepository,
    private val logRepository: LogRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ScanBarcodeUiState())
    val uiState: StateFlow<ScanBarcodeUiState> = _uiState.asStateFlow()

    fun onBarcodeDetected(barcode: String) {
        val normalizedBarcode = barcode.trim()
        if (normalizedBarcode.isBlank() || _uiState.value.isLookingUp) {
            return
        }
        lookupBarcode(
            barcode = normalizedBarcode,
            action = "scan_barcode_lookup",
        )
    }

    fun retryLookup() {
        val barcode = _uiState.value.lastBarcode ?: return
        lookupBarcode(
            barcode = barcode,
            action = "scan_barcode_retry",
        )
    }

    fun clearPreviewAndResume() {
        _uiState.update { current ->
            current.copy(
                previewFood = null,
                errorMessage = null,
            )
        }
    }

    fun savePreviewFood() {
        val previewFood = _uiState.value.previewFood ?: return
        viewModelScope.launch {
            try {
                foodRepository.upsertFood(previewFood.toLocalFoodItem())
                _uiState.update { current ->
                    current.copy(
                        previewFood = null,
                        message = "Saved to My Foods.",
                    )
                }
            } catch (_: Exception) {
                _uiState.update { current ->
                    current.copy(message = "Could not save food. Please try again.")
                }
            }
        }
    }

    fun saveAndLogPreviewFood(
        mealType: MealType,
        quantity: Double,
        unit: FoodUnit,
    ) {
        if (!EntryValidation.isValidQuantity(quantity)) {
            _uiState.update { current ->
                current.copy(message = "Enter a valid quantity greater than 0.")
            }
            return
        }
        val previewFood = _uiState.value.previewFood ?: return
        viewModelScope.launch {
            try {
                val localFood = previewFood.toLocalFoodItem()
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
                _uiState.update { current ->
                    current.copy(
                        previewFood = null,
                        message = "Saved and logged.",
                    )
                }
            } catch (_: Exception) {
                _uiState.update { current ->
                    current.copy(message = "Could not save and log food. Please try again.")
                }
            }
        }
    }

    fun consumeMessage() {
        _uiState.update { current -> current.copy(message = null) }
    }

    private fun lookupBarcode(
        barcode: String,
        action: String,
    ) {
        viewModelScope.launch {
            _uiState.update { current ->
                current.copy(
                    isLookingUp = true,
                    errorMessage = null,
                    lastBarcode = barcode,
                )
            }
            try {
                val token = userInitiatedNetworkGuard.issueToken(action)
                val result = remoteFoodDataSource.lookupByBarcode(
                    barcode = barcode,
                    token = token,
                )
                if (result == null) {
                    _uiState.update { current ->
                        current.copy(
                            isLookingUp = false,
                            previewFood = null,
                            errorMessage = "No matching food found for barcode.",
                        )
                    }
                } else {
                    _uiState.update { current ->
                        current.copy(
                            isLookingUp = false,
                            previewFood = result,
                            errorMessage = null,
                        )
                    }
                }
            } catch (_: Exception) {
                _uiState.update { current ->
                    current.copy(
                        isLookingUp = false,
                        previewFood = null,
                        errorMessage = "Lookup failed. Check connection and retry.",
                    )
                }
            }
        }
    }
}

data class ScanBarcodeUiState(
    val lastBarcode: String? = null,
    val isLookingUp: Boolean = false,
    val previewFood: RemoteFoodCandidate? = null,
    val errorMessage: String? = null,
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
