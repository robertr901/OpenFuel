package com.openfuel.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openfuel.app.domain.model.DietaryOverlay
import com.openfuel.app.domain.model.GoalProfile
import com.openfuel.app.domain.model.GoalProfileEmphasis
import com.openfuel.app.domain.model.InsightWindow
import com.openfuel.app.ui.components.OFCard
import com.openfuel.app.ui.components.OFMetricRow
import com.openfuel.app.ui.components.OFPill
import com.openfuel.app.ui.components.OFPrimaryButton
import com.openfuel.app.ui.components.OFSectionHeader
import com.openfuel.app.ui.components.ProPaywallDialog
import com.openfuel.app.ui.theme.Dimens
import com.openfuel.app.ui.util.formatCalories
import com.openfuel.app.ui.util.formatMacro
import com.openfuel.app.viewmodel.InsightsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel,
    onOpenWeeklyReview: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.testTag("screen_insights"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Insights",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Dimens.m),
            verticalArrangement = Arrangement.spacedBy(Dimens.m),
        ) {
            val goalProfile = uiState.goalProfile
            if (goalProfile != null) {
                GoalProfileFocusCard(
                    profile = goalProfile,
                    overlays = uiState.goalProfileOverlays,
                    emphasis = uiState.goalProfileEmphasis,
                )
            }
            if (uiState.showWeeklyReviewEntry) {
                InsightsWeeklyReviewEntryCard(onOpenWeeklyReview = onOpenWeeklyReview)
            }
            if (!uiState.isPro) {
                OFCard(modifier = Modifier.fillMaxWidth()) {
                    OFSectionHeader(
                        title = "Insights is a Pro feature.",
                        subtitle = "Enable Pro in debug settings to preview this screen.",
                    )
                    OFPrimaryButton(
                        text = "See Pro options",
                        onClick = viewModel::openPaywallForGatedFeature,
                        testTag = "insights_open_paywall_button",
                    )
                }
            } else {
                OFCard(modifier = Modifier.fillMaxWidth()) {
                    OFSectionHeader(
                        title = "Consistency score",
                        subtitle = "Calculated locally from your logged days.",
                    )
                    OFMetricRow(
                        label = "Score",
                        value = "${uiState.snapshot.consistencyScore}/100",
                    )
                    OFPill(text = "Local-only")
                }
                if (uiState.snapshot.last30Days.loggedDays == 0) {
                    OFCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No logged days yet. Start logging meals to unlock trends.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                InsightWindowCard(
                    window = uiState.snapshot.last7Days,
                    emphasis = uiState.goalProfileEmphasis,
                )
                InsightWindowCard(
                    window = uiState.snapshot.last30Days,
                    emphasis = uiState.goalProfileEmphasis,
                )
            }
        }

        ProPaywallDialog(
            show = uiState.showPaywall,
            isActionInProgress = uiState.isEntitlementActionInProgress,
            message = uiState.entitlementActionMessage,
            onDismiss = viewModel::dismissPaywall,
            onPurchaseClick = viewModel::purchasePro,
            onRestoreClick = viewModel::restorePurchases,
        )
    }
}

@Composable
private fun InsightsWeeklyReviewEntryCard(
    onOpenWeeklyReview: () -> Unit,
) {
    OFCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("insights_weekly_review_entry_card"),
    ) {
        OFSectionHeader(
            title = "Weekly review",
            subtitle = "See your last 7 days with one practical next step.",
        )
        OFPrimaryButton(
            text = "Open weekly review",
            onClick = onOpenWeeklyReview,
            testTag = "insights_open_weekly_review_button",
        )
    }
}

@Composable
private fun InsightWindowCard(
    window: InsightWindow,
    emphasis: GoalProfileEmphasis?,
) {
    OFCard(modifier = Modifier.fillMaxWidth()) {
        OFSectionHeader(title = window.label)
        OFMetricRow(
            label = "Logged days",
            value = window.loggedDays.toString(),
        )
        OFMetricRow(
            label = "Calories avg",
            value = "${formatCalories(window.average.caloriesKcal)} kcal",
        )
        val macroRows = when (emphasis) {
            GoalProfileEmphasis.PROTEIN -> listOf(
                "Protein avg" to "${formatMacro(window.average.proteinG)} g",
                "Carbs avg" to "${formatMacro(window.average.carbsG)} g",
                "Fat avg" to "${formatMacro(window.average.fatG)} g",
            )
            GoalProfileEmphasis.CARBS -> listOf(
                "Carbs avg" to "${formatMacro(window.average.carbsG)} g",
                "Protein avg" to "${formatMacro(window.average.proteinG)} g",
                "Fat avg" to "${formatMacro(window.average.fatG)} g",
            )
            else -> listOf(
                "Protein avg" to "${formatMacro(window.average.proteinG)} g",
                "Carbs avg" to "${formatMacro(window.average.carbsG)} g",
                "Fat avg" to "${formatMacro(window.average.fatG)} g",
            )
        }
        macroRows.forEach { (label, value) ->
            OFMetricRow(
                label = label,
                value = value,
            )
        }
    }
}

@Composable
private fun GoalProfileFocusCard(
    profile: GoalProfile,
    overlays: Set<DietaryOverlay>,
    emphasis: GoalProfileEmphasis?,
) {
    OFCard(modifier = Modifier.fillMaxWidth()) {
        OFSectionHeader(
            title = "Profile focus",
            subtitle = profile.displayLabel(),
        )
        OFPill(
            text = when (emphasis) {
                GoalProfileEmphasis.CALORIES -> "Calories target focus"
                GoalProfileEmphasis.PROTEIN -> "Protein target focus"
                GoalProfileEmphasis.CARBS -> "Carbs target focus"
                GoalProfileEmphasis.BALANCED -> "Balanced target focus"
                null -> "Balanced target focus"
            },
            testTag = "insights_goal_profile_focus",
        )
        if (overlays.isNotEmpty()) {
            OFPill(
                text = "Overlays: ${overlays.sortedBy { it.name }.joinToString { it.displayLabel() }}",
            )
        }
    }
}

private fun GoalProfile.displayLabel(): String {
    return when (this) {
        GoalProfile.FAT_LOSS -> "Fat loss"
        GoalProfile.MUSCLE_GAIN -> "Muscle gain"
        GoalProfile.MAINTENANCE -> "Maintenance"
        GoalProfile.BLOOD_SUGAR_AWARENESS -> "Blood sugar awareness"
    }
}

private fun DietaryOverlay.displayLabel(): String {
    return when (this) {
        DietaryOverlay.LOW_FODMAP -> "Low FODMAP"
        DietaryOverlay.LOW_SODIUM -> "Low sodium"
    }
}
