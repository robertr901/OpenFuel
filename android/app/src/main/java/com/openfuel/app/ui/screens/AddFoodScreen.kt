package com.openfuel.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.MealType
import com.openfuel.app.ui.components.MealTypeDropdown
import com.openfuel.app.ui.theme.Dimens
import com.openfuel.app.ui.util.formatCalories
import com.openfuel.app.ui.util.formatMacro
import com.openfuel.app.viewmodel.AddFoodViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodScreen(
    viewModel: AddFoodViewModel,
    onNavigateBack: () -> Unit,
    onOpenFoodDetail: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var searchInput by rememberSaveable { mutableStateOf(uiState.searchQuery) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add food") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Navigate back",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = Dimens.m),
            verticalArrangement = Arrangement.spacedBy(Dimens.l),
        ) {
            item {
                QuickAddCard(
                    onQuickAdd = { input ->
                        handleQuickAdd(
                            input = input,
                            viewModel = viewModel,
                            scope = scope,
                            snackbarHostState = snackbarHostState,
                        )
                    },
                )
            }
            item {
                Text(
                    text = "Search / select food",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            item {
                OutlinedTextField(
                    value = searchInput,
                    onValueChange = {
                        searchInput = it
                        viewModel.updateSearchQuery(it)
                    },
                    label = { Text("Search foods") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (uiState.foods.isEmpty()) {
                item {
                    Text(
                        text = if (searchInput.isBlank()) {
                            "No recent foods yet"
                        } else {
                            "No foods match your search"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(uiState.foods, key = { it.id }) { food ->
                    RecentFoodRow(
                        food = food,
                        onLog = { mealType ->
                            viewModel.logFood(
                                foodId = food.id,
                                mealType = mealType,
                                quantity = 1.0,
                                unit = FoodUnit.SERVING,
                            )
                            scope.launch {
                                snackbarHostState.showSnackbar("Logged ${food.name}")
                            }
                        },
                        onOpenDetail = { onOpenFoodDetail(food.id) },
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(Dimens.xl))
            }
        }
    }
}

private data class QuickAddInput(
    val name: String,
    val calories: String,
    val protein: String,
    val carbs: String,
    val fat: String,
    val mealType: MealType,
)

@Composable
private fun QuickAddCard(
    onQuickAdd: (QuickAddInput) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var calories by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("") }
    var carbs by rememberSaveable { mutableStateOf("") }
    var fat by rememberSaveable { mutableStateOf("") }
    var mealType by rememberSaveable { mutableStateOf(MealType.BREAKFAST) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.m),
            verticalArrangement = Arrangement.spacedBy(Dimens.s),
        ) {
            Text(
                text = "Quick add",
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Food name") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = calories,
                onValueChange = { calories = it },
                label = { Text("Calories (kcal)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.s),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = protein,
                    onValueChange = { protein = it },
                    label = { Text("Protein (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = carbs,
                    onValueChange = { carbs = it },
                    label = { Text("Carbs (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = fat,
                    onValueChange = { fat = it },
                    label = { Text("Fat (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }
            MealTypeDropdown(
                selected = mealType,
                onSelected = { mealType = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    onQuickAdd(
                        QuickAddInput(
                            name = name,
                            calories = calories,
                            protein = protein,
                            carbs = carbs,
                            fat = fat,
                            mealType = mealType,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Log quick add",
                )
                Spacer(modifier = Modifier.width(Dimens.s))
                Text("Log quick add")
            }
        }
    }
}

private fun handleQuickAdd(
    input: QuickAddInput,
    viewModel: AddFoodViewModel,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
) {
    val caloriesValue = input.calories.toDoubleOrNull()
    if (caloriesValue == null) {
        scope.launch { snackbarHostState.showSnackbar("Enter calories") }
        return
    }
    val proteinValue = input.protein.toDoubleOrNull() ?: 0.0
    val carbsValue = input.carbs.toDoubleOrNull() ?: 0.0
    val fatValue = input.fat.toDoubleOrNull() ?: 0.0
    viewModel.quickAdd(
        name = input.name,
        caloriesKcal = caloriesValue,
        proteinG = proteinValue,
        carbsG = carbsValue,
        fatG = fatValue,
        mealType = input.mealType,
    )
    scope.launch { snackbarHostState.showSnackbar("Quick add logged") }
}

@Composable
private fun RecentFoodRow(
    food: FoodItem,
    onLog: (MealType) -> Unit,
    onOpenDetail: () -> Unit,
) {
    var selectedMeal by remember { mutableStateOf(MealType.BREAKFAST) }
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.m),
            verticalArrangement = Arrangement.spacedBy(Dimens.s),
        ) {
            Text(text = food.name, style = MaterialTheme.typography.titleMedium)
            if (!food.brand.isNullOrBlank()) {
                Text(
                    text = food.brand ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "${formatCalories(food.caloriesKcal)} kcal · ${formatMacro(food.proteinG)}p ${formatMacro(food.carbsG)}c ${formatMacro(food.fatG)}f",
                style = MaterialTheme.typography.bodySmall,
            )
            MealTypeDropdown(
                selected = selectedMeal,
                onSelected = { selectedMeal = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.s),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = { onLog(selectedMeal) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Log")
                }
                Button(
                    onClick = onOpenDetail,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Details")
                }
            }
        }
    }
}
