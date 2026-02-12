package com.openfuel.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openfuel.app.domain.model.DietaryOverlay
import com.openfuel.app.domain.model.DailyGoal
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.GoalProfile
import com.openfuel.app.domain.model.GoalProfileDefaults
import com.openfuel.app.domain.model.GoalProfileEmphasis
import com.openfuel.app.domain.model.MealType
import com.openfuel.app.domain.model.displayName
import com.openfuel.app.domain.model.shortLabel
import com.openfuel.app.domain.util.EntryValidation
import com.openfuel.app.ui.components.EmptyState
import com.openfuel.app.ui.components.MetricPill
import com.openfuel.app.ui.components.PillKind
import com.openfuel.app.ui.components.OFPrimaryButton
import com.openfuel.app.ui.components.OFMetricRow
import com.openfuel.app.ui.components.OFRow
import com.openfuel.app.ui.components.SectionHeader
import com.openfuel.app.ui.components.StandardCard
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
    onOpenWeeklyReview: () -> Unit,
    onOpenFoodDetail: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var editEntry by remember { mutableStateOf<MealEntryUi?>(null) }
    var deleteEntry by remember { mutableStateOf<MealEntryUi?>(null) }
    var isTotalsDetailsExpanded by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val mealsWithEntries = remember(uiState.meals) {
        uiState.meals.filter { it.entries.isNotEmpty() }
    }
    val emptyMealSlots = remember(uiState.meals) {
        uiState.meals.filter { it.entries.isEmpty() }
    }
    var showEmptyMealSlots by rememberSaveable { mutableStateOf(false) }

    val formattedDate = remember(uiState.date) {
        uiState.date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
    }

    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }

    LaunchedEffect(uiState.showFastLogReminder) {
        if (uiState.showFastLogReminder) {
            viewModel.onFastLogReminderShown()
        }
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

    if (uiState.showGoalProfileOnboarding) {
        GoalProfileOnboardingDialog(
            onApply = { profile, overlays ->
                viewModel.saveGoalProfileSelection(
                    profile = profile,
                    overlays = overlays,
                )
            },
            onSkip = viewModel::skipGoalProfileSelection,
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
            if (uiState.showFastLogReminder) {
                item {
                    FastLogReminderCard(
                        onLogNow = {
                            viewModel.onFastLogReminderActioned()
                            onAddFood()
                        },
                        onDismiss = viewModel::dismissFastLogReminder,
                    )
                }
            }
            item {
                TotalsCard(
                    calories = uiState.totals.caloriesKcal,
                    protein = uiState.totals.proteinG,
                    carbs = uiState.totals.carbsG,
                    fat = uiState.totals.fatG,
                    goal = uiState.goal,
                    goalProfile = uiState.goalProfile,
                    goalProfileOverlays = uiState.goalProfileOverlays,
                    goalProfileEmphasis = uiState.goalProfileEmphasis,
                    isDetailsExpanded = isTotalsDetailsExpanded,
                    onToggleDetails = { isTotalsDetailsExpanded = !isTotalsDetailsExpanded },
                )
            }
            if (uiState.showWeeklyReviewEntry) {
                item {
                    WeeklyReviewEntryCard(onOpenWeeklyReview = onOpenWeeklyReview)
                }
            }
            if (mealsWithEntries.isEmpty()) {
                item {
                    EmptyDayState(onAddFood = onAddFood)
                }
            }
            if (mealsWithEntries.isNotEmpty()) {
                items(mealsWithEntries, key = { it.mealType.name }) { meal ->
                    MealSection(
                        modifier = Modifier
                            .animateContentSize()
                            .testTag("home_meal_sections_logged"),
                        mealSection = meal,
                        onEntryClick = onOpenFoodDetail,
                        onEditEntry = { editEntry = it },
                        onDeleteEntry = { deleteEntry = it },
                    )
                }
            }
            if (emptyMealSlots.isNotEmpty()) {
                item {
                    EmptyMealSlotsCard(
                        emptyMealSlots = emptyMealSlots,
                        isExpanded = showEmptyMealSlots,
                        onToggleExpanded = { showEmptyMealSlots = !showEmptyMealSlots },
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(Dimens.xl))
            }
        }
    }
}

@Composable
private fun WeeklyReviewEntryCard(
    onOpenWeeklyReview: () -> Unit,
) {
    StandardCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_weekly_review_entry_card"),
    ) {
        SectionHeader(
            title = "Weekly review",
            subtitle = "Review your last 7 days in under 30 seconds.",
            modifier = Modifier.semantics { heading() },
        )
        OFPrimaryButton(
            text = "Open weekly review",
            onClick = onOpenWeeklyReview,
            modifier = Modifier.fillMaxWidth(),
            testTag = "home_open_weekly_review_button",
        )
    }
}

@Composable
private fun EmptyDayState(
    onAddFood: () -> Unit,
) {
    EmptyState(
        title = "No meals logged yet",
        body = "Tap Add food to start logging this day.",
        modifier = Modifier.fillMaxWidth(),
        icon = Icons.Rounded.Add,
        primaryAction = {
            OFPrimaryButton(
                text = "Add food",
                onClick = onAddFood,
                modifier = Modifier.fillMaxWidth(),
                testTag = "home_primary_log_action",
            )
        },
        testTag = "home_empty_day_state",
    )
}

@Composable
private fun EmptyMealSlotsCard(
    emptyMealSlots: List<MealSectionUi>,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
) {
    StandardCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Empty meal slots",
            subtitle = "Show sections with no entries.",
            modifier = Modifier.semantics { heading() },
            trailing = {
                TextButton(
                    onClick = onToggleExpanded,
                    modifier = Modifier
                        .semantics {
                            contentDescription = "Toggle empty meal slots"
                        }
                        .testTag("home_empty_meal_sections_toggle"),
                ) {
                    Text(if (isExpanded) "Hide" else "Show")
                }
            },
        )
        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_empty_meal_sections_content"),
                verticalArrangement = Arrangement.spacedBy(Dimens.xs),
            ) {
                emptyMealSlots.forEach { mealSection ->
                    OFRow(
                        title = mealSection.mealType.displayName(),
                        subtitle = "No entries yet",
                        contentDescription = "${mealSection.mealType.displayName()} has no entries yet",
                    )
                }
            }
        }
    }
}

@Composable
private fun FastLogReminderCard(
    onLogNow: () -> Unit,
    onDismiss: () -> Unit,
) {
    StandardCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_fast_log_reminder_card"),
    ) {
        SectionHeader(
            title = "Fast log reminder",
            subtitle = "No meals logged today. Add a meal in under a minute.",
            modifier = Modifier.semantics { heading() },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.s),
        ) {
            OFPrimaryButton(
                text = "Open add food",
                onClick = onLogNow,
                modifier = Modifier.weight(1f),
                testTag = "home_fast_log_reminder_action",
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("home_fast_log_reminder_dismiss"),
            ) {
                Text("Dismiss")
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
    goal: DailyGoal?,
    goalProfile: GoalProfile?,
    goalProfileOverlays: Set<DietaryOverlay>,
    goalProfileEmphasis: GoalProfileEmphasis?,
    isDetailsExpanded: Boolean,
    onToggleDetails: () -> Unit,
) {
    StandardCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
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
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
            MetricPill(text = "Protein ${formatMacro(protein)}g", kind = PillKind.DEFAULT)
            MetricPill(text = "Carbs ${formatMacro(carbs)}g", kind = PillKind.DEFAULT)
            MetricPill(text = "Fat ${formatMacro(fat)}g", kind = PillKind.DEFAULT)
        }
        if (goalProfile != null) {
            val emphasisLabel = when (goalProfileEmphasis) {
                GoalProfileEmphasis.CALORIES -> "Calories target focus"
                GoalProfileEmphasis.PROTEIN -> "Protein target focus"
                GoalProfileEmphasis.CARBS -> "Carbs target focus"
                GoalProfileEmphasis.BALANCED -> "Balanced target focus"
                null -> "Balanced target focus"
            }
            OFRow(
                title = "Profile: ${goalProfile.displayLabel()}",
                subtitle = emphasisLabel,
                testTag = "home_goal_profile_summary",
            )
            if (goalProfileOverlays.isNotEmpty()) {
                OFRow(
                    title = "Overlays: ${goalProfileOverlays.sortedBy { it.name }.joinToString { it.displayLabel() }}",
                    subtitle = "UI guidance labels only",
                )
            }
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
private fun GoalProfileOnboardingDialog(
    onApply: (GoalProfile, Set<DietaryOverlay>) -> Unit,
    onSkip: () -> Unit,
) {
    var selectedProfile by rememberSaveable { mutableStateOf(GoalProfile.MAINTENANCE) }
    var lowFodmapEnabled by rememberSaveable { mutableStateOf(false) }
    var lowSodiumEnabled by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        modifier = Modifier.testTag("goal_profile_onboarding_dialog"),
        onDismissRequest = {},
        title = { Text("Choose your goal profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.s)) {
                Text(
                    text = "This takes under 20 seconds. You can change it anytime in Settings.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                GoalProfileOption(
                    profile = GoalProfile.FAT_LOSS,
                    selected = selectedProfile == GoalProfile.FAT_LOSS,
                    onSelect = { selectedProfile = GoalProfile.FAT_LOSS },
                    testTag = "goal_profile_option_fat_loss",
                )
                GoalProfileOption(
                    profile = GoalProfile.MUSCLE_GAIN,
                    selected = selectedProfile == GoalProfile.MUSCLE_GAIN,
                    onSelect = { selectedProfile = GoalProfile.MUSCLE_GAIN },
                    testTag = "goal_profile_option_muscle_gain",
                )
                GoalProfileOption(
                    profile = GoalProfile.MAINTENANCE,
                    selected = selectedProfile == GoalProfile.MAINTENANCE,
                    onSelect = { selectedProfile = GoalProfile.MAINTENANCE },
                    testTag = "goal_profile_option_maintenance",
                )
                GoalProfileOption(
                    profile = GoalProfile.BLOOD_SUGAR_AWARENESS,
                    selected = selectedProfile == GoalProfile.BLOOD_SUGAR_AWARENESS,
                    onSelect = { selectedProfile = GoalProfile.BLOOD_SUGAR_AWARENESS },
                    testTag = "goal_profile_option_blood_sugar_awareness",
                )
                OverlayToggle(
                    title = DietaryOverlay.LOW_FODMAP.displayLabel(),
                    enabled = lowFodmapEnabled,
                    onEnabledChange = { lowFodmapEnabled = it },
                    testTag = "goal_profile_overlay_low_fodmap_toggle",
                )
                OverlayToggle(
                    title = DietaryOverlay.LOW_SODIUM.displayLabel(),
                    enabled = lowSodiumEnabled,
                    onEnabledChange = { lowSodiumEnabled = it },
                    testTag = "goal_profile_overlay_low_sodium_toggle",
                )
                Text(
                    text = GoalProfileDefaults.NON_CLINICAL_DISCLAIMER,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                modifier = Modifier.testTag("goal_profile_apply_button"),
                onClick = {
                    val overlays = buildSet {
                        if (lowFodmapEnabled) add(DietaryOverlay.LOW_FODMAP)
                        if (lowSodiumEnabled) add(DietaryOverlay.LOW_SODIUM)
                    }
                    onApply(selectedProfile, overlays)
                },
            ) {
                Text("Apply profile")
            }
        },
        dismissButton = {
            TextButton(
                modifier = Modifier.testTag("goal_profile_skip_button"),
                onClick = onSkip,
            ) {
                Text("Skip for now")
            }
        },
    )
}

@Composable
private fun GoalProfileOption(
    profile: GoalProfile,
    selected: Boolean,
    onSelect: () -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.s),
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
        )
        Text(
            text = profile.displayLabel(),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun OverlayToggle(
    title: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    testTag: String,
) {
    OFRow(
        title = title,
        subtitle = "Label only. No diagnosis or treatment guidance.",
        trailing = {
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                modifier = Modifier.testTag(testTag),
            )
        },
    )
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
    modifier: Modifier = Modifier,
    mealSection: MealSectionUi,
    onEntryClick: (String) -> Unit,
    onEditEntry: (MealEntryUi) -> Unit,
    onDeleteEntry: (MealEntryUi) -> Unit,
) {
    Column(modifier = modifier) {
        SectionHeader(
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
                    Spacer(modifier = Modifier.height(Dimens.s))
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
    StandardCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = Dimens.m,
            vertical = Dimens.sm,
        ),
    ) {
        OFRow(
            title = entry.name,
            subtitle = "${formatQuantity(entry.quantity)} ${entry.unit.shortLabel()}",
            trailing = {
                Text(
                    text = "${formatCalories(entry.caloriesKcal)} kcal",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
            TextButton(onClick = onEdit) {
                Text("Edit")
            }
            TextButton(onClick = onDelete) {
                Text("Delete")
            }
        }
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

private fun GoalProfile.displayLabel(): String {
    return when (this) {
        GoalProfile.FAT_LOSS -> "Fat loss"
        GoalProfile.MUSCLE_GAIN -> "Muscle gain"
        GoalProfile.MAINTENANCE -> "Maintenance"
        GoalProfile.BLOOD_SUGAR_AWARENESS -> "Blood sugar awareness"
    }
}

private fun DietaryOverlay.displayLabel(): String {
    return when (this) {
        DietaryOverlay.LOW_FODMAP -> "Low FODMAP"
        DietaryOverlay.LOW_SODIUM -> "Low sodium"
    }
}
