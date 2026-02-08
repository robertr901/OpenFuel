package com.openfuel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfuel.app.domain.model.EntitlementActionResult
import com.openfuel.app.domain.model.InsightsSnapshot
import com.openfuel.app.domain.repository.LogRepository
import com.openfuel.app.domain.service.EntitlementService
import com.openfuel.app.domain.util.InsightsCalculator
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InsightsViewModel(
    private val entitlementService: EntitlementService,
    logRepository: LogRepository,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
    private val today: LocalDate = LocalDate.now(clock.withZone(zoneId))
    private val paywallUiState = MutableStateFlow(InsightsPaywallUiState())

    val uiState: StateFlow<InsightsUiState> = combine(
        entitlementService.getEntitlementState(),
        logRepository.entriesInRange(
            startDate = today.minusDays(29),
            endDateInclusive = today,
            zoneId = zoneId,
        ),
        paywallUiState,
    ) { entitlementState, entries, paywall ->
        InsightsUiState(
            isPro = entitlementState.isPro,
            snapshot = InsightsCalculator.buildSnapshot(entries, today, zoneId),
            showPaywall = paywall.showPaywall,
            isEntitlementActionInProgress = paywall.isActionInProgress,
            entitlementActionMessage = paywall.message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InsightsUiState(
            isPro = false,
            snapshot = InsightsSnapshot.empty(today),
            showPaywall = false,
            isEntitlementActionInProgress = false,
            entitlementActionMessage = null,
        ),
    )

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

data class InsightsUiState(
    val isPro: Boolean,
    val snapshot: InsightsSnapshot,
    val showPaywall: Boolean,
    val isEntitlementActionInProgress: Boolean,
    val entitlementActionMessage: String?,
)

private data class InsightsPaywallUiState(
    val showPaywall: Boolean = false,
    val isActionInProgress: Boolean = false,
    val message: String? = null,
)
