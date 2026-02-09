package com.openfuel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfuel.app.domain.model.DailyGoal
import com.openfuel.app.domain.model.EntitlementActionResult
import com.openfuel.app.domain.repository.GoalsRepository
import com.openfuel.app.domain.repository.SettingsRepository
import com.openfuel.app.domain.service.EntitlementService
import com.openfuel.app.domain.service.FoodCatalogProviderDescriptor
import com.openfuel.app.domain.service.FoodCatalogProviderRegistry
import com.openfuel.app.domain.service.ProviderExecutionSnapshot
import com.openfuel.app.domain.service.ProviderExecutionDiagnosticsStore
import com.openfuel.app.domain.util.GoalValidation
import com.openfuel.app.export.AdvancedExportPreview
import com.openfuel.app.export.ExportFormat
import com.openfuel.app.export.ExportManager
import com.openfuel.app.export.ExportRedactionOptions
import java.io.File
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val entitlementService: EntitlementService,
    private val goalsRepository: GoalsRepository,
    private val exportManager: ExportManager,
    private val foodCatalogProviderRegistry: FoodCatalogProviderRegistry,
    private val providerExecutionDiagnosticsStore: ProviderExecutionDiagnosticsStore,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
    private val exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    private val paywallUiState = MutableStateFlow(PaywallUiState())
    private val advancedExportState = MutableStateFlow<AdvancedExportState>(AdvancedExportState.Idle)
    private val advancedExportFormat = MutableStateFlow(ExportFormat.JSON)
    private val advancedExportRedacted = MutableStateFlow(false)
    private val advancedExportPreview = MutableStateFlow(AdvancedExportPreview.empty())

    private val baseUiState = combine(
        settingsRepository.onlineLookupEnabled,
        entitlementService.getEntitlementState(),
        exportState,
        goalsRepository.goalForDate(today()),
        providerExecutionDiagnosticsStore.latestExecution,
    ) { onlineLookupEnabled, entitlementState, exportStateValue, dailyGoal, providerExecutionSnapshot ->
        SettingsUiState(
            onlineLookupEnabled = onlineLookupEnabled,
            isPro = entitlementState.isPro,
            showDebugProToggle = entitlementState.canToggleDebugOverride,
            showSecurityWarning = entitlementState.canToggleDebugOverride &&
                (entitlementState.securityPosture.isEmulator || entitlementState.securityPosture.hasTestKeys),
            providerDiagnostics = foodCatalogProviderRegistry.providerDiagnostics(
                onlineLookupEnabled = onlineLookupEnabled,
            ),
            lastProviderExecution = providerExecutionSnapshot,
            exportState = exportStateValue,
            dailyGoal = dailyGoal,
        )
    }

    private val baseWithPaywallUiState = combine(
        baseUiState,
        paywallUiState,
    ) { base, paywall ->
        base.copy(
            showPaywall = paywall.showPaywall,
            isEntitlementActionInProgress = paywall.isActionInProgress,
            entitlementActionMessage = paywall.message,
        )
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        baseWithPaywallUiState,
        advancedExportState,
        advancedExportFormat,
        advancedExportRedacted,
        advancedExportPreview,
    ) { base, advancedState, format, redacted, preview ->
        base.copy(
            advancedExportState = advancedState,
            advancedExportFormat = format,
            advancedExportRedacted = redacted,
            advancedExportPreview = preview,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(
            onlineLookupEnabled = true,
            isPro = false,
            showDebugProToggle = false,
            showSecurityWarning = false,
            providerDiagnostics = emptyList(),
            lastProviderExecution = null,
            exportState = ExportState.Idle,
            dailyGoal = null,
            showPaywall = false,
            isEntitlementActionInProgress = false,
            entitlementActionMessage = null,
            advancedExportState = AdvancedExportState.Idle,
            advancedExportFormat = ExportFormat.JSON,
            advancedExportRedacted = false,
            advancedExportPreview = AdvancedExportPreview.empty(),
        ),
    )

    init {
        viewModelScope.launch {
            entitlementService.refreshEntitlements()
        }
        refreshAdvancedExportPreview()
    }

    fun setOnlineLookupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setOnlineLookupEnabled(enabled)
        }
    }

    fun setProEnabled(enabled: Boolean) {
        viewModelScope.launch {
            entitlementService.setDebugProOverride(enabled)
        }
    }

    fun openPaywall() {
        paywallUiState.value = paywallUiState.value.copy(
            showPaywall = true,
            message = null,
        )
    }

    fun dismissPaywall() {
        paywallUiState.value = paywallUiState.value.copy(showPaywall = false)
    }

    fun purchasePro() {
        runEntitlementAction { entitlementService.purchasePro() }
    }

    fun restorePurchases() {
        runEntitlementAction { entitlementService.restorePurchases() }
    }

    fun consumeEntitlementMessage() {
        paywallUiState.value = paywallUiState.value.copy(message = null)
    }

    fun saveGoals(
        caloriesTarget: Double?,
        proteinTarget: Double?,
        carbsTarget: Double?,
        fatTarget: Double?,
    ): GoalSaveResult {
        if (caloriesTarget != null && !GoalValidation.isValidCalories(caloriesTarget)) {
            return GoalSaveResult.Error("Calories must be between 0 and 10000.")
        }
        if (proteinTarget != null && !GoalValidation.isValidMacro(proteinTarget)) {
            return GoalSaveResult.Error("Protein must be between 0 and 1000g.")
        }
        if (carbsTarget != null && !GoalValidation.isValidMacro(carbsTarget)) {
            return GoalSaveResult.Error("Carbs must be between 0 and 1000g.")
        }
        if (fatTarget != null && !GoalValidation.isValidMacro(fatTarget)) {
            return GoalSaveResult.Error("Fat must be between 0 and 1000g.")
        }

        val goal = DailyGoal(
            date = today(),
            caloriesKcalTarget = caloriesTarget ?: 0.0,
            proteinGTarget = proteinTarget ?: 0.0,
            carbsGTarget = carbsTarget ?: 0.0,
            fatGTarget = fatTarget ?: 0.0,
        )
        viewModelScope.launch {
            goalsRepository.upsertGoal(goal)
        }
        return GoalSaveResult.Success
    }

    fun exportData(cacheDir: File, appVersion: String) {
        if (exportState.value is ExportState.Exporting) return
        viewModelScope.launch {
            exportState.value = ExportState.Exporting
            try {
                val file = exportManager.export(cacheDir, appVersion)
                exportState.value = ExportState.Success(file)
            } catch (exception: Exception) {
                exportState.value = ExportState.Error("Export failed. Please try again.")
            }
        }
    }

    fun consumeExport() {
        exportState.value = ExportState.Idle
    }

    fun setAdvancedExportFormat(format: ExportFormat) {
        advancedExportFormat.value = format
    }

    fun setAdvancedExportRedacted(enabled: Boolean) {
        advancedExportRedacted.value = enabled
        refreshAdvancedExportPreview()
    }

    fun exportAdvancedData(cacheDir: File, appVersion: String) {
        if (!uiState.value.isPro) {
            paywallUiState.value = paywallUiState.value.copy(
                showPaywall = true,
                message = "Advanced export is available on Pro.",
            )
            return
        }
        if (advancedExportState.value is AdvancedExportState.Exporting) return

        viewModelScope.launch {
            advancedExportState.value = AdvancedExportState.Exporting
            try {
                val file = exportManager.exportAdvanced(
                    cacheDir = cacheDir,
                    appVersion = appVersion,
                    format = advancedExportFormat.value,
                    redactionOptions = ExportRedactionOptions(
                        redactBrand = advancedExportRedacted.value,
                    ),
                )
                advancedExportState.value = AdvancedExportState.Success(file)
            } catch (_: Exception) {
                advancedExportState.value = AdvancedExportState.Error(
                    "Advanced export failed. Please try again.",
                )
            }
        }
    }

    fun consumeAdvancedExport() {
        advancedExportState.value = AdvancedExportState.Idle
    }

    private fun today(): LocalDate = LocalDate.now(clock)

    private fun refreshAdvancedExportPreview() {
        viewModelScope.launch {
            advancedExportPreview.value = try {
                exportManager.previewAdvancedExport(
                    redactionOptions = ExportRedactionOptions(
                        redactBrand = advancedExportRedacted.value,
                    ),
                )
            } catch (_: Exception) {
                AdvancedExportPreview.empty()
            }
        }
    }

    private fun runEntitlementAction(
        action: suspend () -> EntitlementActionResult,
    ) {
        if (paywallUiState.value.isActionInProgress) return
        viewModelScope.launch {
            paywallUiState.value = paywallUiState.value.copy(
                isActionInProgress = true,
                message = null,
            )
            val result = action()
            entitlementService.refreshEntitlements()
            val message = when (result) {
                is EntitlementActionResult.Success -> result.message
                is EntitlementActionResult.Error -> result.message
                EntitlementActionResult.Cancelled -> "Purchase cancelled."
            }
            paywallUiState.value = paywallUiState.value.copy(
                showPaywall = result !is EntitlementActionResult.Success,
                isActionInProgress = false,
                message = message,
            )
        }
    }
}

data class SettingsUiState(
    val onlineLookupEnabled: Boolean = true,
    val isPro: Boolean = false,
    val showDebugProToggle: Boolean = false,
    val showSecurityWarning: Boolean = false,
    val providerDiagnostics: List<FoodCatalogProviderDescriptor> = emptyList(),
    val lastProviderExecution: ProviderExecutionSnapshot? = null,
    val exportState: ExportState = ExportState.Idle,
    val dailyGoal: DailyGoal? = null,
    val showPaywall: Boolean = false,
    val isEntitlementActionInProgress: Boolean = false,
    val entitlementActionMessage: String? = null,
    val advancedExportState: AdvancedExportState = AdvancedExportState.Idle,
    val advancedExportFormat: ExportFormat = ExportFormat.JSON,
    val advancedExportRedacted: Boolean = false,
    val advancedExportPreview: AdvancedExportPreview = AdvancedExportPreview.empty(),
)

sealed class GoalSaveResult {
    data object Success : GoalSaveResult()
    data class Error(val message: String) : GoalSaveResult()
}

sealed class ExportState {
    data object Idle : ExportState()
    data object Exporting : ExportState()
    data class Success(val file: File) : ExportState()
    data class Error(val message: String) : ExportState()
}

sealed class AdvancedExportState {
    data object Idle : AdvancedExportState()
    data object Exporting : AdvancedExportState()
    data class Success(val file: File) : AdvancedExportState()
    data class Error(val message: String) : AdvancedExportState()
}

private data class PaywallUiState(
    val showPaywall: Boolean = false,
    val isActionInProgress: Boolean = false,
    val message: String? = null,
)
