package com.openfuel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfuel.app.domain.repository.LogRepository
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(
    logRepository: LogRepository,
    zoneId: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {
    val uiState: StateFlow<HistoryUiState> = logRepository.loggedDates(zoneId)
        .map { dates ->
            HistoryUiState(
                days = dates,
                isEmpty = dates.isEmpty(),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState(days = emptyList(), isEmpty = true),
        )
}

data class HistoryUiState(
    val days: List<LocalDate>,
    val isEmpty: Boolean,
)
