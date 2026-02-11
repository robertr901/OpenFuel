package com.openfuel.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openfuel.app.domain.model.DailyGoal
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.MealType
import com.openfuel.app.domain.model.displayName
import com.openfuel.app.domain.model.shortLabel
import com.openfuel.app.domain.util.EntryValidation
import com.openfuel.app.ui.components.OFCard
import com.openfuel.app.ui.components.OFEmptyState
import com.openfuel.app.ui.components.OFMetricRow
import com.openfuel.app.ui.components.OFSectionHeader
import com.openfuel.app.ui.components.OFStatPill
import com.openfuel.app.ui.components.MealTypeDropdown
import com.openfuel.app.ui.components.UnitDropdown
import com.openfuel.app.ui.theme.Dimens
import com.openfuel.app.ui.util.formatCalories
import com.openfuel.app.ui.util.formatMacro
import com.openfuel.app.ui.util.formatQuantity
import com.openfuel.app.ui.util.parseDecimalInput
import com.openfuel.app.viewmodel.HomeViewModel
import com.openfuel.app.viewmodel.MealEntryUi
import com.openfuel.app.viewmodel.MealSectionUi
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAddFood: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFoodDetail: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var editEntry by remember { mutableStateOf<MealEntryUi?>(null) }
    var deleteEntry by remember { mutableStateOf<MealEntryUi?>(null) }
    var isTotalsDetailsExpanded by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val formattedDate = remember(uiState.date) {
        uiState.date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
    }

    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }

    if (editEntry != null) {
        EditMealEntryDialog(
            entry = editEntry!!,
            onDismiss = { editEntry = null },
            onSave = { quantity, unit, mealType ->
                viewModel.updateEntry(
                    entry = editEntry!!,
                    quantity = quantity,
                    unit = unit,
                    mealType = mealType,
                )
                editEntry = null
            },
        )
    }

    if (deleteEntry != null) {
        DeleteMealEntryDialog(
            entry = deleteEntry!!,
            onDismiss = { deleteEntry = null },
            onConfirmDelete = {
                viewModel.deleteEntry(deleteEntry!!.id)
                deleteEntry = null
            },
        )
    }

    Scaffold(
        modifier = Modifier.testTag("screen_today"),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = viewModel::goToPreviousDay) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Previous day",
                        )
                    }
                },
                title = {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = {
                    IconButton(onClick = viewModel::goToNextDay) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Next day",
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
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
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add food",
                    )
                },
                onClick = onAddFood,
                modifier = Modifier.testTag("home_add_food_fab"),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    goal = uiState.goal,
                    isDetailsExpanded = isTotalsDetailsExpanded,
                    onToggleDetails = { isTotalsDetailsExpanded = !isTotalsDetailsExpanded },
                )
            }
            if (uiState.meals.all { it.entries.isEmpty() }) {
                item {
                    EmptyDayState()
                }
            }
            items(uiState.meals, key = { it.mealType.name }) { meal ->
                MealSection(
                    mealSection = meal,
                    onEntryClick = onOpenFoodDetail,
                    onEditEntry = { editEntry = it },
                    onDeleteEntry = { deleteEntry = it },
                )
            }
            item {
                Spacer(modifier = Modifier.height(Dimens.xl))
            }
        }
    }
}

@Composable
private fun EmptyDayState() {
    OFEmptyState(
        title = "No meals logged yet",
        body = "Tap Add food to start logging this day.",
        modifier = Modifier.fillMaxWidth(),
        icon = Icons.Rounded.Add,
    )
}

@Composable
private fun TotalsCard(
    calories: Double,
    protein: Double,
    carbs: Double,
    fat: Double,
    goal: DailyGoal?,
    isDetailsExpanded: Boolean,
    onToggleDetails: () -> Unit,
) {
    OFCard(modifier = Modifier.fillMaxWidth()) {
        OFSectionHeader(
            title = "Daily totals",
            subtitle = "Clear progress at a glance.",
            modifier = Modifier.semantics { heading() },
            trailing = {
                TextButton(
                    onClick = onToggleDetails,
                    modifier = Modifier.testTag("home_totals_details_toggle"),
                ) {
                    Text(if (isDetailsExpanded) "Hide details" else "Show details")
                }
            },
        )
        Text(
            text = "${formatCalories(calories)} kcal",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
            OFStatPill(text = "P ${formatMacro(protein)}g")
            OFStatPill(text = "C ${formatMacro(carbs)}g")
            OFStatPill(text = "F ${formatMacro(fat)}g")
        }
        AnimatedVisibility(visible = isDetailsExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_totals_details_content"),
                verticalArrangement = Arrangement.spacedBy(Dimens.s),
            ) {
                OFMetricRow(label = "Protein", value = "${formatMacro(protein)} g")
                OFMetricRow(label = "Carbs", value = "${formatMacro(carbs)} g")
                OFMetricRow(label = "Fat", value = "${formatMacro(fat)} g")
                if (goal != null) {
                    if (goal.caloriesKcalTarget > 0.0) {
                        GoalProgressRow(
                            label = "Calories",
                            consumed = calories,
                            target = goal.caloriesKcalTarget,
                            unit = "kcal",
                        )
                    }
                    if (goal.proteinGTarget > 0.0) {
                        GoalProgressRow(
                            label = "Protein",
                            consumed = protein,
                            target = goal.proteinGTarget,
                            unit = "g",
                        )
                    }
                    if (goal.carbsGTarget > 0.0) {
                        GoalProgressRow(
                            label = "Carbs",
                            consumed = carbs,
                            target = goal.carbsGTarget,
                            unit = "g",
                        )
                    }
                    if (goal.fatGTarget > 0.0) {
                        GoalProgressRow(
                            label = "Fat",
                            consumed = fat,
                            target = goal.fatGTarget,
                            unit = "g",
                        )
                    }
                }
            }
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
private fun GoalProgressRow(
    label: String,
    consumed: Double,
    target: Double,
    unit: String,
) {
    val progress = (consumed / target).coerceIn(0.0, 1.0).toFloat()
    val consumedValue = if (unit == "kcal") formatCalories(consumed) else formatMacro(consumed)
    val targetValue = if (unit == "kcal") formatCalories(target) else formatMacro(target)
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.xs)) {
        Text(
            text = "$label $consumedValue/$targetValue $unit",
            style = MaterialTheme.typography.bodySmall,
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MealSection(
    mealSection: MealSectionUi,
    onEntryClick: (String) -> Unit,
    onEditEntry: (MealEntryUi) -> Unit,
    onDeleteEntry: (MealEntryUi) -> Unit,
) {
    Column {
        OFSectionHeader(
            title = mealSection.mealType.displayName(),
            subtitle = "${formatCalories(mealSection.totals.caloriesKcal)} kcal",
            modifier = Modifier.semantics { heading() },
        )
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
                    onEdit = { onEditEntry(entry) },
                    onDelete = { onDeleteEntry(entry) },
                )
                if (index < mealSection.entries.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.s))
                }
            }
        }
    }
}

@Composable
private fun MealEntryRow(
    entry: MealEntryUi,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
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
            OFStatPill(text = "${formatQuantity(entry.quantity)} ${entry.unit.shortLabel()}")
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                TextButton(onClick = onEdit) {
                    Text("Edit")
                }
                TextButton(onClick = onDelete) {
                    Text("Delete")
                }
            }
        }
        Text(
            text = "${formatCalories(entry.caloriesKcal)} kcal",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun EditMealEntryDialog(
    entry: MealEntryUi,
    onDismiss: () -> Unit,
    onSave: (Double, FoodUnit, MealType) -> Unit,
) {
    var quantityInput by rememberSaveable(entry.id) { mutableStateOf(formatQuantity(entry.quantity)) }
    var selectedUnit by rememberSaveable(entry.id) { mutableStateOf(entry.unit) }
    var selectedMealType by rememberSaveable(entry.id) { mutableStateOf(entry.mealType) }

    val quantityValue = parseDecimalInput(quantityInput)
    val validQuantity = quantityValue?.let { EntryValidation.isValidQuantity(it) } ?: false

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit meal entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.s)) {
                OutlinedTextField(
                    value = quantityInput,
                    onValueChange = { quantityInput = it },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = quantityInput.isNotBlank() && !validQuantity,
                    supportingText = {
                        if (quantityInput.isNotBlank() && !validQuantity) {
                            Text("Quantity must be greater than 0")
                        }
                    },
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
            TextButton(
                onClick = {
                    if (quantityValue != null) {
                        onSave(quantityValue, selectedUnit, selectedMealType)
                    }
                },
                enabled = validQuantity,
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun DeleteMealEntryDialog(
    entry: MealEntryUi,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete entry") },
        text = { Text("Delete ${entry.name} from your log?") },
        confirmButton = {
            TextButton(onClick = onConfirmDelete) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
