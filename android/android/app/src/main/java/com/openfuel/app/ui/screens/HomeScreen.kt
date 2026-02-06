package com.openfuel.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openfuel.app.domain.model.displayName
import com.openfuel.app.domain.model.shortLabel
import com.openfuel.app.ui.theme.Dimens
import com.openfuel.app.ui.util.formatCalories
import com.openfuel.app.ui.util.formatMacro
import com.openfuel.app.ui.util.formatQuantity
import com.openfuel.app.viewmodel.HomeViewModel
import com.openfuel.app.viewmodel.MealEntryUi
import com.openfuel.app.viewmodel.MealSectionUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAddFood: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFoodDetail: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Today") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Open settings",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Add food") },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add food",
                    )
                },
                onClick = onAddFood,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimens.m),
            verticalArrangement = Arrangement.spacedBy(Dimens.m),
        ) {
            item {
                TotalsCard(
                    calories = uiState.totals.caloriesKcal,
                    protein = uiState.totals.proteinG,
                    carbs = uiState.totals.carbsG,
                    fat = uiState.totals.fatG,
                )
            }
            items(uiState.meals, key = { it.mealType.name }) { meal ->
                MealSection(
                    mealSection = meal,
                    onEntryClick = onOpenFoodDetail,
                )
            }
            item {
                Spacer(modifier = Modifier.height(Dimens.xl))
            }
        }
    }
}

@Composable
private fun TotalsCard(
    calories: Double,
    protein: Double,
    carbs: Double,
    fat: Double,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Dimens.m)) {
            Text(
                text = "Daily totals",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(Dimens.s))
            Text(
                text = "${formatCalories(calories)} kcal",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(Dimens.s))
            MacroRow(label = "Protein", value = protein)
            MacroRow(label = "Carbs", value = carbs)
            MacroRow(label = "Fat", value = fat)
        }
    }
}

@Composable
private fun MacroRow(label: String, value: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = "${formatMacro(value)} g", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun MealSection(
    mealSection: MealSectionUi,
    onEntryClick: (String) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = mealSection.mealType.displayName(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${formatCalories(mealSection.totals.caloriesKcal)} kcal",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(modifier = Modifier.height(Dimens.s))
        if (mealSection.entries.isEmpty()) {
            Text(
                text = "No entries yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            mealSection.entries.forEachIndexed { index, entry ->
                MealEntryRow(
                    entry = entry,
                    onClick = { onEntryClick(entry.foodId) },
                )
                if (index < mealSection.entries.lastIndex) {
                    Divider(modifier = Modifier.padding(vertical = Dimens.s))
                }
            }
        }
    }
}

@Composable
private fun MealEntryRow(
    entry: MealEntryUi,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Dimens.s),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = entry.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "${formatQuantity(entry.quantity)} ${entry.unit.shortLabel()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "${formatCalories(entry.caloriesKcal)} kcal",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
