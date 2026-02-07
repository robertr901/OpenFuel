package com.openfuel.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openfuel.app.domain.model.InsightWindow
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
        topBar = { TopAppBar(title = { Text("Insights") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Dimens.m),
            verticalArrangement = Arrangement.spacedBy(Dimens.m),
        ) {
            if (!uiState.isPro) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(Dimens.m),
                        verticalArrangement = Arrangement.spacedBy(Dimens.s),
                    ) {
                        Text(
                            text = "Insights is a Pro feature.",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "Enable Pro in debug settings to preview this screen.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                return@Column
            }

            Text(
                text = "Consistency score: ${uiState.snapshot.consistencyScore}/100",
                style = MaterialTheme.typography.titleLarge,
            )
            InsightWindowCard(window = uiState.snapshot.last7Days)
            InsightWindowCard(window = uiState.snapshot.last30Days)
        }
    }
}

@Composable
private fun InsightWindowCard(
    window: InsightWindow,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Dimens.m),
            verticalArrangement = Arrangement.spacedBy(Dimens.xs),
        ) {
            Text(
                text = window.label,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Logged days: ${window.loggedDays}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Calories avg: ${formatCalories(window.average.caloriesKcal)} kcal",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Protein avg: ${formatMacro(window.average.proteinG)} g",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Carbs avg: ${formatMacro(window.average.carbsG)} g",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Fat avg: ${formatMacro(window.average.fatG)} g",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
