package com.openfuel.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.ui.theme.Dimens
import com.openfuel.app.ui.util.formatCalories
import com.openfuel.app.ui.util.formatMacro
import com.openfuel.app.viewmodel.FoodLibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodLibraryScreen(
    viewModel: FoodLibraryViewModel,
    onAddFood: () -> Unit,
    onOpenFoodDetail: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchInput by rememberSaveable { mutableStateOf(uiState.searchQuery) }

    Scaffold(
        modifier = Modifier.testTag("screen_foods"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Foods",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddFood,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add food",
                    )
                },
                text = { Text("Add food") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimens.m),
            verticalArrangement = Arrangement.spacedBy(Dimens.m),
        ) {
            OutlinedTextField(
                value = searchInput,
                onValueChange = {
                    searchInput = it
                    viewModel.updateSearchQuery(it)
                },
                label = { Text("Search foods") },
                modifier = Modifier.fillMaxWidth(),
            )

            if (uiState.foods.isEmpty()) {
                Text(
                    text = if (searchInput.isBlank()) {
                        "No foods saved yet. Add a food to build your library."
                    } else {
                        "No local foods match your search."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(Dimens.s),
                ) {
                    items(uiState.foods, key = { it.id }) { food ->
                        FoodLibraryRow(
                            food = food,
                            onOpenFoodDetail = { onOpenFoodDetail(food.id) },
                        )
                    }
                    item { Spacer(modifier = Modifier.height(Dimens.xl)) }
                }
            }
        }
    }
}

@Composable
private fun FoodLibraryRow(
    food: FoodItem,
    onOpenFoodDetail: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenFoodDetail),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.m),
            verticalArrangement = Arrangement.spacedBy(Dimens.xs),
        ) {
            Text(
                text = food.name,
                style = MaterialTheme.typography.titleMedium,
            )
            if (!food.brand.isNullOrBlank()) {
                Text(
                    text = food.brand.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "${formatCalories(food.caloriesKcal)} kcal - ${formatMacro(food.proteinG)}p ${formatMacro(food.carbsG)}c ${formatMacro(food.fatG)}f",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
