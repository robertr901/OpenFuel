package com.openfuel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfuel.app.data.remote.UserInitiatedNetworkGuard
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.MealEntry
import com.openfuel.app.domain.model.MealType
import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.domain.model.toLocalFoodItem
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
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
    private val nowEpochMillis: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {
    private companion object {
        private const val BARCODE_DEDUPE_WINDOW_MS = 1_500L
    }

    private val _uiState = MutableStateFlow(ScanBarcodeUiState())
    val uiState: StateFlow<ScanBarcodeUiState> = _uiState.asStateFlow()
    private var lookupRequestCounter: Long = 0L
    private var activeLookupRequestId: Long = 0L
    private var activeLookupJob: Job? = null
    private var activeLookupBarcode: String? = null
    private var lastLookupBarcode: String? = null
    private var lastLookupStartedAtMs: Long = Long.MIN_VALUE

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
        if (normalizedBarcode.isBlank()) {
            return
        }
        lookupBarcode(
            barcode = normalizedBarcode,
            action = "scan_barcode_lookup",
            force = false,
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
            force = true,
        )
    }

    fun clearPreviewAndResume() {
        _uiState.update { current ->
            current.copy(
                previewFood = null,
                errorMessage = null,
                providerResults = emptyList(),
                lookupStatus = BarcodeLookupStatus.IDLE,
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
        force: Boolean,
    ) {
        val nowMs = nowEpochMillis()
        if (!force && shouldSkipDuplicateLookup(barcode = barcode, nowMs = nowMs)) {
            return
        }

        _uiState.update { current ->
            current.copy(
                lastBarcode = barcode,
                errorMessage = null,
                lookupStatus = BarcodeLookupStatus.RECEIVED,
            )
        }

        activeLookupJob?.cancel()
        val requestId = nextLookupRequestId()
        activeLookupRequestId = requestId
        activeLookupBarcode = barcode
        lastLookupBarcode = barcode
        lastLookupStartedAtMs = nowMs

        activeLookupJob = viewModelScope.launch {
            _uiState.update { current ->
                if (isStale(requestId)) {
                    return@update current
                }
                current.copy(
                    lookupStatus = BarcodeLookupStatus.LOOKING_UP,
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
                if (isStale(requestId)) {
                    return@launch
                }
                val result = report.mergedCandidates.firstOrNull()?.candidate
                val outcome = deriveBarcodeOutcome(
                    providerResults = report.providerResults,
                    hasResult = result != null,
                )
                if (result == null) {
                    _uiState.update { current ->
                        if (isStale(requestId)) {
                            return@update current
                        }
                        current.copy(
                            lookupStatus = outcome.status,
                            previewFood = current.previewFood,
                            errorMessage = outcome.message ?: "No matching food found for barcode.",
                            providerResults = report.providerResults,
                        )
                    }
                } else {
                    _uiState.update { current ->
                        if (isStale(requestId)) {
                            return@update current
                        }
                        current.copy(
                            lookupStatus = BarcodeLookupStatus.SUCCESS,
                            previewFood = result,
                            errorMessage = null,
                            providerResults = report.providerResults,
                            message = outcome.message ?: current.message,
                        )
                    }
                }
            } catch (_: CancellationException) {
                return@launch
            } catch (exception: Exception) {
                if (isStale(requestId)) {
                    return@launch
                }
                val outcome = mapExceptionToBarcodeOutcome(exception)
                _uiState.update { current ->
                    current.copy(
                        lookupStatus = outcome.status,
                        previewFood = current.previewFood,
                        errorMessage = outcome.message,
                    )
                }
            } finally {
                if (!isStale(requestId)) {
                    activeLookupJob = null
                    activeLookupBarcode = null
                }
            }
        }
    }

    private fun shouldSkipDuplicateLookup(
        barcode: String,
        nowMs: Long,
    ): Boolean {
        if (activeLookupJob?.isActive == true && barcode == activeLookupBarcode) {
            return true
        }
        if (barcode == lastLookupBarcode && lastLookupStartedAtMs != Long.MIN_VALUE) {
            val elapsedMs = nowMs - lastLookupStartedAtMs
            if (elapsedMs in 0 until BARCODE_DEDUPE_WINDOW_MS) {
                return true
            }
        }
        return false
    }

    private fun nextLookupRequestId(): Long {
        lookupRequestCounter += 1
        return lookupRequestCounter
    }

    private fun isStale(requestId: Long): Boolean {
        return requestId != activeLookupRequestId
    }
}

data class ScanBarcodeUiState(
    val lastBarcode: String? = null,
    val lookupStatus: BarcodeLookupStatus = BarcodeLookupStatus.IDLE,
    val onlineLookupEnabled: Boolean = true,
    val previewFood: RemoteFoodCandidate? = null,
    val errorMessage: String? = null,
    val providerResults: List<ProviderResult> = emptyList(),
    val message: String? = null,
) {
    val isLookingUp: Boolean
        get() = lookupStatus == BarcodeLookupStatus.RECEIVED || lookupStatus == BarcodeLookupStatus.LOOKING_UP

    val canRetry: Boolean
        get() = lookupStatus in setOf(
            BarcodeLookupStatus.ERROR,
            BarcodeLookupStatus.NO_CONNECTION,
            BarcodeLookupStatus.TIMEOUT,
        )
}

enum class BarcodeLookupStatus {
    IDLE,
    RECEIVED,
    LOOKING_UP,
    SUCCESS,
    ERROR,
    NO_CONNECTION,
    TIMEOUT,
}

private fun deriveBarcodeOutcome(
    providerResults: List<ProviderResult>,
    hasResult: Boolean,
): BarcodeLookupOutcome {
    if (hasResult) {
        val failedStatuses = setOf(
            ProviderStatus.NETWORK_UNAVAILABLE,
            ProviderStatus.HTTP_ERROR,
            ProviderStatus.PARSING_ERROR,
            ProviderStatus.ERROR,
            ProviderStatus.TIMEOUT,
            ProviderStatus.GUARD_REJECTED,
            ProviderStatus.RATE_LIMITED,
        )
        val hasFailures = providerResults.any { result -> result.status in failedStatuses }
        return if (hasFailures) {
            BarcodeLookupOutcome(
                status = BarcodeLookupStatus.SUCCESS,
                message = "Some providers were unavailable. Showing available match.",
            )
        } else {
            BarcodeLookupOutcome(status = BarcodeLookupStatus.SUCCESS)
        }
    }

    val hasMissingUsdaKey = providerResults.any { result ->
        result.providerId.equals("usda_fdc", ignoreCase = true) &&
            result.status == ProviderStatus.DISABLED_BY_SETTINGS &&
            result.diagnostics?.contains("API key missing", ignoreCase = true) == true
    }
    if (hasMissingUsdaKey) {
        return BarcodeLookupOutcome(
            status = BarcodeLookupStatus.ERROR,
            message = "USDA provider is not configured. Add USDA_API_KEY in local.properties.",
        )
    }

    if (providerResults.any { result -> result.status == ProviderStatus.TIMEOUT }) {
        return BarcodeLookupOutcome(
            status = BarcodeLookupStatus.TIMEOUT,
            message = "Timed out (check connection).",
        )
    }
    if (providerResults.any { result -> result.status == ProviderStatus.NETWORK_UNAVAILABLE }) {
        return BarcodeLookupOutcome(
            status = BarcodeLookupStatus.NO_CONNECTION,
            message = "No connection.",
        )
    }

    val serviceFailureStatuses = setOf(
        ProviderStatus.HTTP_ERROR,
        ProviderStatus.PARSING_ERROR,
        ProviderStatus.ERROR,
        ProviderStatus.GUARD_REJECTED,
        ProviderStatus.RATE_LIMITED,
    )
    if (providerResults.any { result -> result.status in serviceFailureStatuses }) {
        return BarcodeLookupOutcome(
            status = BarcodeLookupStatus.ERROR,
            message = "Service error.",
        )
    }

    return BarcodeLookupOutcome(status = BarcodeLookupStatus.ERROR)
}

private fun mapExceptionToBarcodeOutcome(
    exception: Exception,
): BarcodeLookupOutcome {
    return when (exception.rootCause()) {
        is SocketTimeoutException -> BarcodeLookupOutcome(
            status = BarcodeLookupStatus.TIMEOUT,
            message = "Timed out (check connection).",
        )
        is IOException -> BarcodeLookupOutcome(
            status = BarcodeLookupStatus.NO_CONNECTION,
            message = "No connection.",
        )
        else -> BarcodeLookupOutcome(
            status = BarcodeLookupStatus.ERROR,
            message = "Lookup failed. Check connection and retry.",
        )
    }
}

private fun Throwable.rootCause(): Throwable {
    var current: Throwable = this
    while (current.cause != null && current.cause !== current) {
        current = current.cause!!
    }
    return current
}

private data class BarcodeLookupOutcome(
    val status: BarcodeLookupStatus,
    val message: String? = null,
)
