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
            if (!uiState.isPro) {
                OFCard(modifier = Modifier.fillMaxWidth()) {
                    OFSectionHeader(
                        title = "Insights is a Pro feature.",
                        subtitle = "Enable Pro in debug settings to preview this screen.",
                    )
                    OFPrimaryButton(
                        text = "See Pro options",
                        onClick = viewModel::openPaywall,
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
                InsightWindowCard(window = uiState.snapshot.last7Days)
                InsightWindowCard(window = uiState.snapshot.last30Days)
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
private fun InsightWindowCard(
    window: InsightWindow,
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
        OFMetricRow(
            label = "Protein avg",
            value = "${formatMacro(window.average.proteinG)} g",
        )
        OFMetricRow(
            label = "Carbs avg",
            value = "${formatMacro(window.average.carbsG)} g",
        )
        OFMetricRow(
            label = "Fat avg",
            value = "${formatMacro(window.average.fatG)} g",
        )
    }
}
