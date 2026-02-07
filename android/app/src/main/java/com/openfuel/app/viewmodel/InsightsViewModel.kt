package com.openfuel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfuel.app.domain.model.InsightsSnapshot
import com.openfuel.app.domain.repository.LogRepository
import com.openfuel.app.domain.service.EntitlementService
import com.openfuel.app.domain.util.InsightsCalculator
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class InsightsViewModel(
    entitlementService: EntitlementService,
    logRepository: LogRepository,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
    private val today: LocalDate = LocalDate.now(clock.withZone(zoneId))

    val uiState: StateFlow<InsightsUiState> = combine(
        entitlementService.getEntitlementState(),
        logRepository.entriesInRange(
            startDate = today.minusDays(29),
            endDateInclusive = today,
            zoneId = zoneId,
        ),
    ) { entitlementState, entries ->
        InsightsUiState(
            isPro = entitlementState.isPro,
            snapshot = InsightsCalculator.buildSnapshot(entries, today, zoneId),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InsightsUiState(
            isPro = false,
            snapshot = InsightsSnapshot.empty(today),
        ),
    )
}

data class InsightsUiState(
    val isPro: Boolean,
    val snapshot: InsightsSnapshot,
)
