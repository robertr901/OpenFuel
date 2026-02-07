package com.openfuel.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.MealType
import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.domain.search.SearchSourceFilter
import com.openfuel.app.ui.components.MealTypeDropdown
import com.openfuel.app.ui.components.OFCard
import com.openfuel.app.ui.components.OFEmptyState
import com.openfuel.app.ui.components.OFPrimaryButton
import com.openfuel.app.ui.components.OFSectionHeader
import com.openfuel.app.ui.components.OFSecondaryButton
import com.openfuel.app.ui.components.OFStatPill
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
    onScanBarcode: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var searchInput by rememberSaveable { mutableStateOf(uiState.searchQuery) }

    LaunchedEffect(uiState.searchQuery) {
        if (uiState.searchQuery != searchInput) {
            searchInput = uiState.searchQuery
        }
    }

    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }

    Scaffold(
        modifier = Modifier.testTag("screen_add_food"),
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
                UnifiedSearchControls(
                    query = searchInput,
                    sourceFilter = uiState.sourceFilter,
                    isOnlineSearchInProgress = uiState.isOnlineSearchInProgress,
                    onQueryChange = { newQuery ->
                        searchInput = newQuery
                        viewModel.updateSearchQuery(newQuery)
                    },
                    onSourceFilterChange = viewModel::setSourceFilter,
                    onSearchOnline = viewModel::searchOnline,
                    onScanBarcode = onScanBarcode,
                )
            }

            val queryBlank = searchInput.isBlank()
            if (queryBlank) {
                item {
                    OFSectionHeader(
                        title = "Recent logs",
                        subtitle = "Fastest way to repeat your usual foods.",
                    )
                }
                if (uiState.recentLoggedFoods.isEmpty()) {
                    item {
                        OFEmptyState(
                            title = "No recent foods yet",
                            body = "Log a meal once and it will appear here for quick reuse.",
                        )
                    }
                } else {
                    items(uiState.recentLoggedFoods, key = { "recent-${it.id}" }) { food ->
                        SearchResultFoodRow(
                            food = food,
                            sourceLabel = "Recent",
                            onLog = { mealType ->
                                logFoodFromRow(
                                    viewModel = viewModel,
                                    scope = scope,
                                    snackbarHostState = snackbarHostState,
                                    food = food,
                                    mealType = mealType,
                                )
                            },
                            onOpenDetail = { onOpenFoodDetail(food.id) },
                        )
                    }
                }

                item {
                    OFSectionHeader(
                        title = "Favorites",
                        subtitle = "Pin foods you log often.",
                    )
                }
                if (uiState.favoriteFoods.isEmpty()) {
                    item {
                        OFEmptyState(
                            title = "No favorites yet",
                            body = "Star foods in details and they will show up here.",
                        )
                    }
                } else {
                    items(uiState.favoriteFoods, key = { "favorite-${it.id}" }) { food ->
                        SearchResultFoodRow(
                            food = food,
                            sourceLabel = "Favorite",
                            onLog = { mealType ->
                                logFoodFromRow(
                                    viewModel = viewModel,
                                    scope = scope,
                                    snackbarHostState = snackbarHostState,
                                    food = food,
                                    mealType = mealType,
                                )
                            },
                            onOpenDetail = { onOpenFoodDetail(food.id) },
                        )
                    }
                }
            } else {
                val showLocalSection = uiState.sourceFilter != SearchSourceFilter.ONLINE_ONLY
                val showOnlineSection = uiState.sourceFilter != SearchSourceFilter.LOCAL_ONLY

                if (showLocalSection) {
                    item {
                        OFSectionHeader(
                            title = "Local results",
                            subtitle = "Instant matches from foods already on this device.",
                            modifier = Modifier.testTag("add_food_unified_local_section"),
                        )
                    }
                    if (uiState.foods.isEmpty()) {
                        item {
                            OFEmptyState(
                                title = "No local matches",
                                body = "Try a broader search phrase or use Search online.",
                            )
                        }
                    } else {
                        items(uiState.foods, key = { "local-${it.id}" }) { food ->
                            SearchResultFoodRow(
                                food = food,
                                sourceLabel = "Local",
                                onLog = { mealType ->
                                    logFoodFromRow(
                                        viewModel = viewModel,
                                        scope = scope,
                                        snackbarHostState = snackbarHostState,
                                        food = food,
                                        mealType = mealType,
                                    )
                                },
                                onOpenDetail = { onOpenFoodDetail(food.id) },
                            )
                        }
                    }
                }

                if (showOnlineSection) {
                    item {
                        OFSectionHeader(
                            title = "Online results",
                            subtitle = "Fetched from Open Food Facts when you tap Search online.",
                            modifier = Modifier.testTag("add_food_unified_online_section"),
                        )
                    }
                    if (!uiState.onlineLookupEnabled) {
                        item {
                            Text(
                                text = "Online search is off. You can enable it in Settings any time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("add_food_unified_online_disabled_hint"),
                            )
                        }
                    } else if (!uiState.hasSearchedOnline && !uiState.isOnlineSearchInProgress && uiState.onlineErrorMessage == null) {
                        item {
                            OFEmptyState(
                                title = "Ready to search online",
                                body = "Tap Search online to fetch matching foods.",
                                modifier = Modifier.testTag("add_food_unified_online_idle_hint"),
                            )
                        }
                    }

                    if (uiState.isOnlineSearchInProgress) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.testTag("add_food_unified_online_loading"),
                                )
                            }
                        }
                    }

                    if (uiState.onlineErrorMessage != null) {
                        item {
                            Text(
                                text = uiState.onlineErrorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }

                    if (uiState.hasSearchedOnline && uiState.onlineResults.isEmpty() && !uiState.isOnlineSearchInProgress && uiState.onlineErrorMessage == null) {
                        item {
                            OFEmptyState(
                                title = "No online matches found",
                                body = "No results for \"$searchInput\".",
                                modifier = Modifier.testTag("add_food_unified_online_empty_state"),
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
                }
            }

            item {
                Spacer(modifier = Modifier.height(Dimens.xl))
            }
        }
    }

    val selectedOnlineFood = uiState.selectedOnlineFood
    if (selectedOnlineFood != null) {
        OnlineFoodPreviewDialog(
            food = selectedOnlineFood,
            onDismiss = { viewModel.closeOnlineFoodPreview() },
            onSave = { viewModel.saveOnlineFood(selectedOnlineFood) },
            onSaveAndLog = { quantity, unit, mealType ->
                viewModel.saveAndLogOnlineFood(
                    food = selectedOnlineFood,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnifiedSearchControls(
    query: String,
    sourceFilter: SearchSourceFilter,
    isOnlineSearchInProgress: Boolean,
    onQueryChange: (String) -> Unit,
    onSourceFilterChange: (SearchSourceFilter) -> Unit,
    onSearchOnline: () -> Unit,
    onScanBarcode: () -> Unit,
) {
    OFCard {
        OFSectionHeader(
            title = "Unified search",
            subtitle = "Search your local foods first, then fetch online when needed.",
        )
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Search foods") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_food_unified_query_input"),
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SearchSourceFilter.entries.forEachIndexed { index, filter ->
                SegmentedButton(
                    selected = sourceFilter == filter,
                    onClick = { onSourceFilterChange(filter) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = SearchSourceFilter.entries.size,
                    ),
                    modifier = Modifier.testTag(
                        when (filter) {
                            SearchSourceFilter.ALL -> "add_food_filter_all"
                            SearchSourceFilter.LOCAL_ONLY -> "add_food_filter_local"
                            SearchSourceFilter.ONLINE_ONLY -> "add_food_filter_online"
                        },
                    ),
                    label = {
                        Text(
                            when (filter) {
                                SearchSourceFilter.ALL -> "All"
                                SearchSourceFilter.LOCAL_ONLY -> "Local"
                                SearchSourceFilter.ONLINE_ONLY -> "Online"
                            },
                        )
                    },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.s),
        ) {
            OFPrimaryButton(
                text = "Search online",
                onClick = onSearchOnline,
                enabled = query.isNotBlank() && !isOnlineSearchInProgress,
                modifier = Modifier
                    .weight(1f)
                    .testTag("add_food_unified_search_online"),
            )
            OFSecondaryButton(
                text = "Scan barcode",
                onClick = onScanBarcode,
                modifier = Modifier
                    .weight(1f)
                    .testTag("add_food_unified_scan_barcode"),
            )
        }
    }
}

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

    OFCard {
        OFSectionHeader(
            title = "Quick add",
            subtitle = "Fast manual entry for one-off meals.",
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.s),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Food name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_food_quick_name_input"),
            )
            OutlinedTextField(
                value = calories,
                onValueChange = { calories = it },
                label = { Text("Calories (kcal)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_food_quick_calories_input"),
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
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_food_quick_protein_input"),
                )
                OutlinedTextField(
                    value = carbs,
                    onValueChange = { carbs = it },
                    label = { Text("Carbs (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_food_quick_carbs_input"),
                )
                OutlinedTextField(
                    value = fat,
                    onValueChange = { fat = it },
                    label = { Text("Fat (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_food_quick_fat_input"),
                )
            }
            MealTypeDropdown(
                selected = mealType,
                onSelected = { mealType = it },
                modifier = Modifier.fillMaxWidth(),
            )
            OFPrimaryButton(
                text = "Log quick add",
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
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_food_quick_log_button"),
            )
        }
    }
}

private fun handleQuickAdd(
    input: QuickAddInput,
    viewModel: AddFoodViewModel,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
) {
    val maxCalories = 10_000.0
    val maxMacro = 1_000.0
    val caloriesValue = parseDecimalInput(input.calories)
    if (caloriesValue == null) {
        scope.launch { snackbarHostState.showSnackbar("Enter calories") }
        return
    }
    val proteinValue = parseDecimalInput(input.protein) ?: 0.0
    val carbsValue = parseDecimalInput(input.carbs) ?: 0.0
    val fatValue = parseDecimalInput(input.fat) ?: 0.0
    if (caloriesValue !in 0.0..maxCalories) {
        scope.launch { snackbarHostState.showSnackbar("Calories must be between 0 and 10000.") }
        return
    }
    if (proteinValue !in 0.0..maxMacro || carbsValue !in 0.0..maxMacro || fatValue !in 0.0..maxMacro) {
        scope.launch { snackbarHostState.showSnackbar("Macros must be between 0 and 1000 g.") }
        return
    }
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

private fun logFoodFromRow(
    viewModel: AddFoodViewModel,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    food: FoodItem,
    mealType: MealType,
) {
    viewModel.logFood(
        foodId = food.id,
        mealType = mealType,
        quantity = 1.0,
        unit = FoodUnit.SERVING,
    )
    scope.launch {
        snackbarHostState.showSnackbar("Logged ${food.name}")
    }
}

@Composable
private fun SearchResultFoodRow(
    food: FoodItem,
    sourceLabel: String,
    onLog: (MealType) -> Unit,
    onOpenDetail: () -> Unit,
) {
    var selectedMeal by remember { mutableStateOf(MealType.BREAKFAST) }
    OFCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.s),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = food.name, style = MaterialTheme.typography.titleMedium)
                OFStatPill(text = sourceLabel)
            }
            if (!food.brand.isNullOrBlank()) {
                Text(
                    text = food.brand.orEmpty(),
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
                OFPrimaryButton(
                    text = "Log",
                    onClick = { onLog(selectedMeal) },
                    modifier = Modifier.weight(1f),
                )
                OFSecondaryButton(
                    text = "Details",
                    onClick = onOpenDetail,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun OnlineResultRow(
    food: RemoteFoodCandidate,
    onOpenPreview: () -> Unit,
) {
    OFCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.s),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                OFStatPill(text = "Online")
            }
            if (!food.brand.isNullOrBlank()) {
                Text(
                    text = food.brand.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val calories = food.caloriesKcalPer100g
            val protein = food.proteinGPer100g
            val carbs = food.carbsGPer100g
            val fat = food.fatGPer100g
            if (calories == null && protein == null && carbs == null && fat == null) {
                Text(
                    text = "Nutrition unknown",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = "${formatCalories(calories ?: 0.0)} kcal · ${formatMacro(protein ?: 0.0)}p ${formatMacro(carbs ?: 0.0)}c ${formatMacro(fat ?: 0.0)}f per 100g",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            OFSecondaryButton(
                text = "Preview",
                onClick = onOpenPreview,
                modifier = Modifier.fillMaxWidth(),
            )
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
                    text = "Per 100 g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Calories: ${formatCalories(food.caloriesKcalPer100g ?: 0.0)} kcal",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "Protein: ${formatMacro(food.proteinGPer100g ?: 0.0)} g",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "Carbs: ${formatMacro(food.carbsGPer100g ?: 0.0)} g",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "Fat: ${formatMacro(food.fatGPer100g ?: 0.0)} g",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (!food.servingSize.isNullOrBlank()) {
                    Text(
                        text = "Serving info: ${food.servingSize}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    Text(
                        text = "Serving info unavailable. Values are shown per 100 g.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    Text("Save to foods")
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
                    Text("Log now")
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
