package com.openfuel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfuel.app.domain.model.DailyGoal
import com.openfuel.app.domain.repository.GoalsRepository
import com.openfuel.app.domain.repository.SettingsRepository
import com.openfuel.app.domain.service.EntitlementService
import com.openfuel.app.domain.util.GoalValidation
import com.openfuel.app.export.ExportManager
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
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
    private val exportState = MutableStateFlow<ExportState>(ExportState.Idle)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.onlineLookupEnabled,
        entitlementService.getEntitlementState(),
        exportState,
        goalsRepository.goalForDate(today()),
    ) { onlineLookupEnabled, entitlementState, exportStateValue, dailyGoal ->
        SettingsUiState(
            onlineLookupEnabled = onlineLookupEnabled,
            isPro = entitlementState.isPro,
            showDebugProToggle = entitlementState.canToggleDebugOverride,
            showSecurityWarning = entitlementState.canToggleDebugOverride &&
                (entitlementState.securityPosture.isEmulator || entitlementState.securityPosture.hasTestKeys),
            exportState = exportStateValue,
            dailyGoal = dailyGoal,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(
            onlineLookupEnabled = true,
            isPro = false,
            showDebugProToggle = false,
            showSecurityWarning = false,
            exportState = ExportState.Idle,
            dailyGoal = null,
        ),
    )

    init {
        viewModelScope.launch {
            entitlementService.refreshEntitlements()
        }
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

    private fun today(): LocalDate = LocalDate.now(clock)
}

data class SettingsUiState(
    val onlineLookupEnabled: Boolean,
    val isPro: Boolean,
    val showDebugProToggle: Boolean,
    val showSecurityWarning: Boolean,
    val exportState: ExportState,
    val dailyGoal: DailyGoal?,
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
