package com.openfuel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfuel.app.domain.repository.SettingsRepository
import com.openfuel.app.export.ExportManager
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val exportManager: ExportManager,
) : ViewModel() {
    private val exportState = MutableStateFlow<ExportState>(ExportState.Idle)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.onlineLookupEnabled,
        exportState,
    ) { onlineLookupEnabled, exportStateValue ->
        SettingsUiState(
            onlineLookupEnabled = onlineLookupEnabled,
            exportState = exportStateValue,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(
            onlineLookupEnabled = false,
            exportState = ExportState.Idle,
        ),
    )

    fun setOnlineLookupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setOnlineLookupEnabled(enabled)
        }
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
}

data class SettingsUiState(
    val onlineLookupEnabled: Boolean,
    val exportState: ExportState,
)

sealed class ExportState {
    data object Idle : ExportState()
    data object Exporting : ExportState()
    data class Success(val file: File) : ExportState()
    data class Error(val message: String) : ExportState()
}
