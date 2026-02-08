package com.openfuel.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.MealType
import com.openfuel.app.ui.components.MealTypeDropdown
import com.openfuel.app.ui.components.UnitDropdown
import com.openfuel.app.ui.theme.Dimens
import com.openfuel.app.ui.util.formatCalories
import com.openfuel.app.ui.util.formatMacro
import com.openfuel.app.ui.util.parseDecimalInput
import com.openfuel.app.viewmodel.FoodDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodDetailScreen(
    viewModel: FoodDetailViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Food detail") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Navigate back",
                        )
                    }
                },
                actions = {
                    if (uiState.food != null) {
                        IconButton(
                            onClick = viewModel::toggleFavorite,
                        ) {
                            Icon(
                                imageVector = if (uiState.food?.isFavorite == true) {
                                    Icons.Rounded.Favorite
                                } else {
                                    Icons.Rounded.FavoriteBorder
                                },
                                contentDescription = if (uiState.food?.isFavorite == true) {
                                    "Remove from favorites"
                                } else {
                                    "Add to favorites"
                                },
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (uiState.food == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(Dimens.m),
                verticalArrangement = Arrangement.spacedBy(Dimens.m),
            ) {
                Text(
                    text = "Food not found",
                    style = MaterialTheme.typography.titleMedium,
                )
                Button(onClick = onNavigateBack) {
                    Text("Go back")
                }
            }
        } else {
            val food = uiState.food ?: return@Scaffold
            FoodDetailContent(
                food = food,
                onLog = viewModel::logFood,
                isReportedIncorrect = food.isReportedIncorrect,
                showReportIncorrect = food.barcode != null,
                onToggleReportIncorrect = viewModel::toggleReportedIncorrect,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(Dimens.m),
            )
        }
    }
}

@Composable
private fun FoodDetailContent(
    food: FoodItem,
    onLog: (Double, FoodUnit, MealType) -> Unit,
    isReportedIncorrect: Boolean,
    showReportIncorrect: Boolean,
    onToggleReportIncorrect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var quantity by rememberSaveable { mutableStateOf("1") }
    var unit by rememberSaveable { mutableStateOf(FoodUnit.SERVING) }
    var mealType by rememberSaveable { mutableStateOf(MealType.BREAKFAST) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimens.m),
    ) {
        Text(text = food.name, style = MaterialTheme.typography.titleLarge)
        if (!food.brand.isNullOrBlank()) {
            Text(
                text = food.brand.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "${formatCalories(food.caloriesKcal)} kcal · ${formatMacro(food.proteinG)}p ${formatMacro(food.carbsG)}c ${formatMacro(food.fatG)}f per serving",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = quantity,
            onValueChange = { quantity = it },
            label = { Text("Quantity") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        UnitDropdown(
            selected = unit,
            onSelected = { unit = it },
            modifier = Modifier.fillMaxWidth(),
        )
        MealTypeDropdown(
            selected = mealType,
            onSelected = { mealType = it },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "If logging grams, macros assume 100g per serving.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (showReportIncorrect) {
            Button(
                onClick = onToggleReportIncorrect,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (isReportedIncorrect) {
                        "Remove incorrect report"
                    } else {
                        "Report incorrect food"
                    },
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.s),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Button(
                onClick = {
                    val value = parseDecimalInput(quantity) ?: 0.0
                    onLog(value, unit, mealType)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Log food")
            }
        }
        Spacer(modifier = Modifier.height(Dimens.xl))
    }
}
