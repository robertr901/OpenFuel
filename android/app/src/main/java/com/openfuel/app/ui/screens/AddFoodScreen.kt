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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.MealType
import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.ui.components.MealTypeDropdown
import com.openfuel.app.ui.components.UnitDropdown
import com.openfuel.app.ui.theme.Dimens
import com.openfuel.app.ui.util.formatCalories
import com.openfuel.app.ui.util.formatMacro
import com.openfuel.app.ui.util.parseDecimalInput
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

    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add food") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                    text = "Local foods",
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
                            "No local foods yet"
                        } else {
                            "No local foods match your search"
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
                Text(
                    text = "Online search",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.s),
                ) {
                    Button(
                        onClick = { viewModel.searchOnline() },
                        enabled = searchInput.isNotBlank() && !uiState.isOnlineSearchInProgress,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Search online")
                    }
                }
            }
            if (uiState.isOnlineSearchInProgress) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            if (uiState.onlineErrorMessage != null) {
                item {
                    Text(
                        text = uiState.onlineErrorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (uiState.onlineResults.isNotEmpty()) {
                items(uiState.onlineResults, key = { "${it.source}:${it.sourceId}" }) { food ->
                    OnlineResultRow(
                        food = food,
                        onOpenPreview = { viewModel.openOnlineFoodPreview(food) },
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(Dimens.xl))
            }
        }
    }

    if (uiState.selectedOnlineFood != null) {
        OnlineFoodPreviewDialog(
            food = uiState.selectedOnlineFood!!,
            onDismiss = { viewModel.closeOnlineFoodPreview() },
            onSave = { viewModel.saveOnlineFood(uiState.selectedOnlineFood!!) },
            onSaveAndLog = { quantity, unit, mealType ->
                viewModel.saveAndLogOnlineFood(
                    food = uiState.selectedOnlineFood!!,
                    quantity = quantity,
                    unit = unit,
                    mealType = mealType,
                )
            },
        )
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
    val caloriesValue = parseDecimalInput(input.calories)
    if (caloriesValue == null) {
        scope.launch { snackbarHostState.showSnackbar("Enter calories") }
        return
    }
    val proteinValue = parseDecimalInput(input.protein) ?: 0.0
    val carbsValue = parseDecimalInput(input.carbs) ?: 0.0
    val fatValue = parseDecimalInput(input.fat) ?: 0.0
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

@Composable
private fun OnlineResultRow(
    food: RemoteFoodCandidate,
    onOpenPreview: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.m),
            verticalArrangement = Arrangement.spacedBy(Dimens.s),
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
                text = "${formatCalories(food.caloriesKcalPer100g ?: 0.0)} kcal · ${formatMacro(food.proteinGPer100g ?: 0.0)}p ${formatMacro(food.carbsGPer100g ?: 0.0)}c ${formatMacro(food.fatGPer100g ?: 0.0)}f per 100g",
                style = MaterialTheme.typography.bodySmall,
            )
            Button(
                onClick = onOpenPreview,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Preview")
            }
        }
    }
}

@Composable
private fun OnlineFoodPreviewDialog(
    food: RemoteFoodCandidate,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onSaveAndLog: (Double, FoodUnit, MealType) -> Unit,
) {
    var quantityInput by rememberSaveable(food.sourceId) { mutableStateOf("1") }
    var selectedUnit by rememberSaveable(food.sourceId) { mutableStateOf(FoodUnit.SERVING) }
    var selectedMealType by rememberSaveable(food.sourceId) { mutableStateOf(MealType.BREAKFAST) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Online food preview") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.s)) {
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
                    text = "${formatCalories(food.caloriesKcalPer100g ?: 0.0)} kcal · ${formatMacro(food.proteinGPer100g ?: 0.0)}p ${formatMacro(food.carbsGPer100g ?: 0.0)}c ${formatMacro(food.fatGPer100g ?: 0.0)}f per 100g",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (!food.servingSize.isNullOrBlank()) {
                    Text(
                        text = "Serving: ${food.servingSize}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                OutlinedTextField(
                    value = quantityInput,
                    onValueChange = { quantityInput = it },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                UnitDropdown(
                    selected = selectedUnit,
                    onSelected = { selectedUnit = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                MealTypeDropdown(
                    selected = selectedMealType,
                    onSelected = { selectedMealType = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                Button(
                    onClick = {
                        onSave()
                        onDismiss()
                    },
                ) {
                    Text("Save to My Foods")
                }
                Button(
                    onClick = {
                        val quantity = parseDecimalInput(quantityInput)
                        if (quantity != null) {
                            onSaveAndLog(quantity, selectedUnit, selectedMealType)
                            onDismiss()
                        }
                    },
                ) {
                    Text("Save and Log")
                }
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
