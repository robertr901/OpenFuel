package com.openfuel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfuel.app.domain.analytics.AnalyticsService
import com.openfuel.app.domain.analytics.NoOpAnalyticsService
import com.openfuel.app.domain.analytics.ProductEventName
import com.openfuel.app.domain.entitlement.PaywallPromptPolicy
import com.openfuel.app.domain.entitlement.PaywallPromptSource
import com.openfuel.app.domain.model.DietaryOverlay
import com.openfuel.app.domain.model.EntitlementActionResult
import com.openfuel.app.domain.model.GoalProfile
import com.openfuel.app.domain.model.GoalProfileDefaults
import com.openfuel.app.domain.model.GoalProfileEmphasis
import com.openfuel.app.domain.model.InsightsSnapshot
import com.openfuel.app.domain.repository.LogRepository
import com.openfuel.app.domain.repository.SettingsRepository
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
    settingsRepository: SettingsRepository,
    private val paywallPromptPolicy: PaywallPromptPolicy,
    private val analyticsService: AnalyticsService = NoOpAnalyticsService,
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
        settingsRepository.goalProfile,
        settingsRepository.goalProfileOverlays,
        paywallUiState,
    ) { entitlementState, entries, goalProfile, goalProfileOverlays, paywall ->
        InsightsUiState(
            isPro = entitlementState.isPro,
            snapshot = InsightsCalculator.buildSnapshot(entries, today, zoneId),
            goalProfile = goalProfile,
            goalProfileOverlays = goalProfileOverlays,
            goalProfileEmphasis = goalProfile?.let(GoalProfileDefaults::emphasisFor),
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
            goalProfile = null,
            goalProfileOverlays = emptySet(),
            goalProfileEmphasis = null,
            showPaywall = false,
            isEntitlementActionInProgress = false,
            entitlementActionMessage = null,
        ),
    )

    fun openPaywall() {
        if (!paywallPromptPolicy.shouldShowPrompt(PaywallPromptSource.SESSION_LIMITED_UPSELL)) return
        paywallUiState.value = paywallUiState.value.copy(
            showPaywall = true,
            message = null,
        )
        trackPaywallShown(surface = "insights")
    }

    fun openPaywallForGatedFeature() {
        if (!paywallPromptPolicy.shouldShowPrompt(PaywallPromptSource.GATED_FEATURE_ENTRY)) return
        paywallUiState.value = paywallUiState.value.copy(
            showPaywall = true,
            message = null,
        )
        trackPaywallShown(surface = "insights")
    }

    fun dismissPaywall() {
        paywallUiState.value = paywallUiState.value.copy(showPaywall = false)
        analyticsService.track(
            ProductEventName.PAYWALL_CANCELLED,
            mapOf(
                "screen" to "insights",
                "surface" to "paywall",
                "result" to "cancelled",
                "session_index" to "0",
            ),
        )
    }

    fun purchasePro() {
        analyticsService.track(
            ProductEventName.PAYWALL_UPGRADE_TAPPED,
            mapOf(
                "screen" to "insights",
                "surface" to "paywall",
                "session_index" to "0",
            ),
        )
        runEntitlementAction { entitlementService.purchasePro() }
    }

    fun restorePurchases() {
        analyticsService.track(
            ProductEventName.PAYWALL_RESTORE_TAPPED,
            mapOf(
                "screen" to "insights",
                "surface" to "paywall",
                "session_index" to "0",
            ),
        )
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

    private fun trackPaywallShown(surface: String) {
        analyticsService.track(
            ProductEventName.PAYWALL_PROMPT_SHOWN,
            mapOf(
                "screen" to "insights",
                "surface" to surface,
                "session_index" to "0",
            ),
        )
    }
}

data class InsightsUiState(
    val isPro: Boolean,
    val snapshot: InsightsSnapshot,
    val goalProfile: GoalProfile?,
    val goalProfileOverlays: Set<DietaryOverlay>,
    val goalProfileEmphasis: GoalProfileEmphasis?,
    val showPaywall: Boolean,
    val isEntitlementActionInProgress: Boolean,
    val entitlementActionMessage: String?,
)

private data class InsightsPaywallUiState(
    val showPaywall: Boolean = false,
    val isActionInProgress: Boolean = false,
    val message: String? = null,
)
