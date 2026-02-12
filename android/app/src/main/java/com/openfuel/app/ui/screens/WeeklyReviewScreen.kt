package com.openfuel.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openfuel.app.ui.components.EmptyState
import com.openfuel.app.ui.components.OFMetricRow
import com.openfuel.app.ui.components.OFPrimaryButton
import com.openfuel.app.ui.components.OFSecondaryButton
import com.openfuel.app.ui.components.OFSectionHeader
import com.openfuel.app.ui.components.StandardCard
import com.openfuel.app.ui.theme.Dimens
import com.openfuel.app.ui.util.formatCalories
import com.openfuel.app.ui.util.formatMacro
import com.openfuel.app.viewmodel.WeeklyReviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyReviewScreen(
    viewModel: WeeklyReviewViewModel,
    onNavigateBack: () -> Unit,
    onReviewAndFixEntries: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.testTag("screen_weekly_review"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Weekly review",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
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
            if (!uiState.isEligible) {
                EmptyState(
                    title = "No weekly data yet",
                    body = "Log meals on at least 3 days to unlock a reliable weekly review.",
                    icon = Icons.Rounded.Insights,
                )
                return@Column
            }

            StandardCard(modifier = Modifier.fillMaxWidth()) {
                OFSectionHeader(
                    title = "This week at a glance",
                    subtitle = "${uiState.summary.startDate} to ${uiState.summary.endDate}",
                )
                OFMetricRow(label = "Logged days", value = uiState.summary.loggedDays.toString())
                OFMetricRow(label = "Missing days", value = uiState.summary.missingDays.toString())
                OFMetricRow(
                    label = "Calories average",
                    value = "${formatCalories(uiState.summary.average.caloriesKcal)} kcal",
                )
                OFMetricRow(
                    label = "Protein average",
                    value = "${formatMacro(uiState.summary.average.proteinG)} g",
                )
                OFMetricRow(
                    label = "Carbs average",
                    value = "${formatMacro(uiState.summary.average.carbsG)} g",
                )
                OFMetricRow(
                    label = "Fat average",
                    value = "${formatMacro(uiState.summary.average.fatG)} g",
                )
            }

            uiState.dataQualityNote?.let { note ->
                StandardCard(modifier = Modifier.fillMaxWidth()) {
                    OFSectionHeader(
                        title = "Data quality",
                        subtitle = note,
                    )
                }
            }

            if (uiState.showInsufficientData) {
                StandardCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("weekly_review_insufficient_state"),
                ) {
                    OFSectionHeader(
                        title = "Insufficient data",
                        subtitle = "Log meals on at least 3 days this week for a practical adjustment.",
                    )
                }
                if (uiState.showReviewAndFixEntries) {
                    OFSecondaryButton(
                        text = "Review and fix entries",
                        onClick = onReviewAndFixEntries,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("weekly_review_review_and_fix_button"),
                    )
                }
            } else {
                uiState.suggestion?.let { suggestion ->
                    StandardCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("weekly_review_suggested_action_card"),
                    ) {
                        OFSectionHeader(
                            title = suggestion.title,
                            subtitle = suggestion.action,
                        )
                        Text(
                            text = suggestion.why,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OFPrimaryButton(
                            text = "Dismiss for this week",
                            onClick = viewModel::dismissSuggestionForCurrentWeek,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "weekly_review_dismiss_action_button",
                        )
                    }
                }
                if (uiState.showReviewAndFixEntries) {
                    OFSecondaryButton(
                        text = "Review and fix entries",
                        onClick = onReviewAndFixEntries,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("weekly_review_review_and_fix_button"),
                    )
                }
            }
        }
    }
}
