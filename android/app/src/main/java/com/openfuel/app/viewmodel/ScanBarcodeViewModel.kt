package com.openfuel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfuel.app.data.remote.UserInitiatedNetworkGuard
import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.MealEntry
import com.openfuel.app.domain.model.MealType
import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.domain.repository.FoodRepository
import com.openfuel.app.domain.repository.LogRepository
import com.openfuel.app.domain.repository.SettingsRepository
import com.openfuel.app.domain.search.SearchSourceFilter
import com.openfuel.app.domain.service.ProviderExecutionRequest
import com.openfuel.app.domain.service.ProviderExecutor
import com.openfuel.app.domain.service.ProviderRefreshPolicy
import com.openfuel.app.domain.service.ProviderRequestType
import com.openfuel.app.domain.service.ProviderResult
import com.openfuel.app.domain.service.ProviderStatus
import com.openfuel.app.domain.util.EntryValidation
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScanBarcodeViewModel(
    private val providerExecutor: ProviderExecutor,
    private val userInitiatedNetworkGuard: UserInitiatedNetworkGuard,
    private val foodRepository: FoodRepository,
    private val logRepository: LogRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ScanBarcodeUiState())
    val uiState: StateFlow<ScanBarcodeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.onlineLookupEnabled.collect { enabled ->
                _uiState.update { current -> current.copy(onlineLookupEnabled = enabled) }
            }
        }
    }

    fun onBarcodeDetected(barcode: String) {
        if (!uiState.value.onlineLookupEnabled) {
            _uiState.update { current ->
                current.copy(message = "Online search is turned off. Enable it in Settings to use barcode lookup.")
            }
            return
        }
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
        if (!uiState.value.onlineLookupEnabled) {
            _uiState.update { current ->
                current.copy(message = "Online search is turned off. Enable it in Settings to use barcode lookup.")
            }
            return
        }
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
                val report = providerExecutor.execute(
                    request = ProviderExecutionRequest(
                        requestType = ProviderRequestType.BARCODE_LOOKUP,
                        sourceFilter = SearchSourceFilter.ONLINE_ONLY,
                        onlineLookupEnabled = uiState.value.onlineLookupEnabled,
                        barcode = barcode,
                        token = token,
                        refreshPolicy = ProviderRefreshPolicy.CACHE_PREFERRED,
                    ),
                )
                val result = report.mergedCandidates.firstOrNull()?.candidate
                val error = deriveBarcodeError(
                    providerResults = report.providerResults,
                    hasResult = result != null,
                )
                if (result == null) {
                    _uiState.update { current ->
                        current.copy(
                            isLookingUp = false,
                            previewFood = null,
                            errorMessage = error ?: "No matching food found for barcode.",
                            providerResults = report.providerResults,
                        )
                    }
                } else {
                    _uiState.update { current ->
                        current.copy(
                            isLookingUp = false,
                            previewFood = result,
                            errorMessage = error,
                            providerResults = report.providerResults,
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
    val onlineLookupEnabled: Boolean = true,
    val previewFood: RemoteFoodCandidate? = null,
    val errorMessage: String? = null,
    val providerResults: List<ProviderResult> = emptyList(),
    val message: String? = null,
)

private fun RemoteFoodCandidate.toLocalFoodItem(): FoodItem {
    val maxCalories = 10_000.0
    val maxMacro = 1_000.0
    return FoodItem(
        id = UUID.randomUUID().toString(),
        name = name,
        brand = brand,
        barcode = barcode,
        caloriesKcal = caloriesKcalPer100g?.coerceIn(0.0, maxCalories) ?: 0.0,
        proteinG = proteinGPer100g?.coerceIn(0.0, maxMacro) ?: 0.0,
        carbsG = carbsGPer100g?.coerceIn(0.0, maxMacro) ?: 0.0,
        fatG = fatGPer100g?.coerceIn(0.0, maxMacro) ?: 0.0,
        isFavorite = false,
        createdAt = Instant.now(),
    )
}

private fun deriveBarcodeError(
    providerResults: List<ProviderResult>,
    hasResult: Boolean,
): String? {
    val failedStatuses = setOf(
        ProviderStatus.NETWORK_UNAVAILABLE,
        ProviderStatus.HTTP_ERROR,
        ProviderStatus.PARSING_ERROR,
        ProviderStatus.ERROR,
        ProviderStatus.TIMEOUT,
        ProviderStatus.GUARD_REJECTED,
        ProviderStatus.RATE_LIMITED,
    )
    val failed = providerResults.any { result -> result.status in failedStatuses }
    if (!failed) {
        return null
    }
    return if (hasResult) {
        "Some providers were unavailable. Showing available match."
    } else {
        "Lookup failed. Check connection and retry."
    }
}
