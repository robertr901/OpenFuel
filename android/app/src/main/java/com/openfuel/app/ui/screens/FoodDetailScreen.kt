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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.MealEntry
import com.openfuel.app.domain.model.MealType
import com.openfuel.app.domain.repository.FoodRepository
import com.openfuel.app.domain.repository.LogRepository
import com.openfuel.app.ui.components.MealTypeDropdown
import com.openfuel.app.ui.components.UnitDropdown
import com.openfuel.app.ui.theme.Dimens
import com.openfuel.app.ui.util.formatCalories
import com.openfuel.app.ui.util.formatMacro
import com.openfuel.app.ui.util.parseDecimalInput
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodDetailScreen(
    foodId: String?,
    foodRepository: FoodRepository,
    logRepository: LogRepository,
    onNavigateBack: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val foodState by produceState<FoodItem?>(initialValue = null, foodId) {
        value = if (foodId.isNullOrBlank()) {
            null
        } else {
            foodRepository.getFoodById(foodId)
        }
    }
    var isFavorite by remember(foodState?.id, foodState?.isFavorite) {
        mutableStateOf(foodState?.isFavorite ?: false)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Food detail") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                        )
                    }
                },
                actions = {
                    if (foodState != null) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        val nextFavorite = !isFavorite
                                        foodRepository.setFavorite(foodState!!.id, nextFavorite)
                                        isFavorite = nextFavorite
                                        snackbarHostState.showSnackbar(
                                            if (nextFavorite) {
                                                "Added to favorites."
                                            } else {
                                                "Removed from favorites."
                                            },
                                        )
                                    } catch (_: Exception) {
                                        snackbarHostState.showSnackbar("Could not update favorite.")
                                    }
                                }
                            },
                        ) {
                            Icon(
                                imageVector = if (isFavorite) {
                                    Icons.Default.Favorite
                                } else {
                                    Icons.Default.FavoriteBorder
                                },
                                contentDescription = if (isFavorite) {
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
        if (foodState == null) {
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
            FoodDetailContent(
                food = foodState!!,
                onLog = { quantity, unit, mealType ->
                    scope.launch {
                        if (quantity <= 0.0) {
                            snackbarHostState.showSnackbar("Enter a valid quantity")
                            return@launch
                        }
                        val entry = MealEntry(
                            id = UUID.randomUUID().toString(),
                            timestamp = Instant.now(),
                            mealType = mealType,
                            foodItemId = foodState!!.id,
                            quantity = quantity,
                            unit = unit,
                        )
                        logRepository.logMealEntry(entry)
                        snackbarHostState.showSnackbar("Logged ${foodState!!.name}")
                    }
                },
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
