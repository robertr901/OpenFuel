package com.openfuel.app.ui.screens

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openfuel.app.BuildConfig
import com.openfuel.app.domain.model.DailyGoal
import com.openfuel.app.domain.util.GoalValidation
import com.openfuel.app.ui.theme.Dimens
import com.openfuel.app.ui.util.formatMacro
import com.openfuel.app.ui.util.parseDecimalInput
import com.openfuel.app.viewmodel.ExportState
import com.openfuel.app.viewmodel.GoalSaveResult
import com.openfuel.app.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: (() -> Unit)?,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showGoalsDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.exportState) {
        val exportState = uiState.exportState
        if (exportState is ExportState.Success) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                exportState.file,
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newUri(context.contentResolver, "OpenFuel export", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share export"))
            viewModel.consumeExport()
        } else if (exportState is ExportState.Error) {
            snackbarHostState.showSnackbar(exportState.message)
        }
    }

    if (showGoalsDialog) {
        GoalsDialog(
            currentGoal = uiState.dailyGoal,
            onDismiss = { showGoalsDialog = false },
            onSave = { calories, protein, carbs, fat ->
                viewModel.saveTodayGoal(calories, protein, carbs, fat)
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Navigate back",
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Dimens.m),
            verticalArrangement = Arrangement.spacedBy(Dimens.m),
        ) {
            Text(
                text = "Privacy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Your logs stay on device. Online lookup is optional.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(text = "Online food lookup", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Disabled by default. Enable if you want online search.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = uiState.onlineLookupEnabled,
                    onCheckedChange = { viewModel.setOnlineLookupEnabled(it) },
                )
            }
            HorizontalDivider()
            Text(
                text = "Daily goals",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Optional targets stored locally on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GoalSummary(goal = uiState.dailyGoal)
            Button(onClick = { showGoalsDialog = true }) {
                Text("Edit goals")
            }
            HorizontalDivider()
            Text(
                text = "Export",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Export all data to a JSON file you can store or share.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when (uiState.exportState) {
                is ExportState.Exporting -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is ExportState.Error -> {
                    Text(
                        text = (uiState.exportState as ExportState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(
                        onClick = {
                            viewModel.exportData(context.cacheDir, BuildConfig.VERSION_NAME)
                        },
                    ) {
                        Text("Retry export")
                    }
                }
                else -> {
                    Button(
                        onClick = {
                            viewModel.exportData(context.cacheDir, BuildConfig.VERSION_NAME)
                        },
                    ) {
                        Text("Export data")
                    }
                }
            }
            Spacer(modifier = Modifier.height(Dimens.xl))
        }
    }
}

@Composable
private fun GoalSummary(goal: DailyGoal?) {
    val caloriesTarget = goal?.caloriesKcalTarget ?: 0.0
    val proteinTarget = goal?.proteinGTarget ?: 0.0
    val carbsTarget = goal?.carbsGTarget ?: 0.0
    val fatTarget = goal?.fatGTarget ?: 0.0

    if (
        caloriesTarget <= 0.0 &&
        proteinTarget <= 0.0 &&
        carbsTarget <= 0.0 &&
        fatTarget <= 0.0
    ) {
        Text(
            text = "No daily goals set.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.xs)) {
        if (caloriesTarget > 0.0) {
            Text(
                text = "Calories: ${formatMacro(caloriesTarget)} kcal",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (proteinTarget > 0.0) {
            Text(
                text = "Protein: ${formatMacro(proteinTarget)} g",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (carbsTarget > 0.0) {
            Text(
                text = "Carbs: ${formatMacro(carbsTarget)} g",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (fatTarget > 0.0) {
            Text(
                text = "Fat: ${formatMacro(fatTarget)} g",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun GoalsDialog(
    currentGoal: DailyGoal?,
    onDismiss: () -> Unit,
    onSave: (Double?, Double?, Double?, Double?) -> GoalSaveResult,
) {
    var caloriesInput by rememberSaveable { mutableStateOf(valueToInput(currentGoal?.caloriesKcalTarget ?: 0.0)) }
    var proteinInput by rememberSaveable { mutableStateOf(valueToInput(currentGoal?.proteinGTarget ?: 0.0)) }
    var carbsInput by rememberSaveable { mutableStateOf(valueToInput(currentGoal?.carbsGTarget ?: 0.0)) }
    var fatInput by rememberSaveable { mutableStateOf(valueToInput(currentGoal?.fatGTarget ?: 0.0)) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit daily goals") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.s)) {
                OutlinedTextField(
                    value = caloriesInput,
                    onValueChange = { caloriesInput = it },
                    label = { Text("Calories (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = proteinInput,
                    onValueChange = { proteinInput = it },
                    label = { Text("Protein g (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = carbsInput,
                    onValueChange = { carbsInput = it },
                    label = { Text("Carbs g (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = fatInput,
                    onValueChange = { fatInput = it },
                    label = { Text("Fat g (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val calories = parseOptionalDouble(caloriesInput)
                    val protein = parseOptionalDouble(proteinInput)
                    val carbs = parseOptionalDouble(carbsInput)
                    val fat = parseOptionalDouble(fatInput)

                    val parseError = when {
                        caloriesInput.isNotBlank() && calories == null -> "Enter a valid calories value."
                        proteinInput.isNotBlank() && protein == null -> "Enter a valid protein value."
                        carbsInput.isNotBlank() && carbs == null -> "Enter a valid carbs value."
                        fatInput.isNotBlank() && fat == null -> "Enter a valid fat value."
                        calories != null && !GoalValidation.isValidCalories(calories) -> "Calories must be between 0 and 10000."
                        protein != null && !GoalValidation.isValidMacro(protein) -> "Protein must be between 0 and 1000g."
                        carbs != null && !GoalValidation.isValidMacro(carbs) -> "Carbs must be between 0 and 1000g."
                        fat != null && !GoalValidation.isValidMacro(fat) -> "Fat must be between 0 and 1000g."
                        else -> null
                    }

                    if (parseError != null) {
                        errorMessage = parseError
                        return@TextButton
                    }

                    when (val result = onSave(calories, protein, carbs, fat)) {
                        is GoalSaveResult.Error -> {
                            errorMessage = result.message
                        }
                        GoalSaveResult.Success -> {
                            onDismiss()
                        }
                    }
                },
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

private fun parseOptionalDouble(raw: String): Double? {
    val value = raw.trim()
    if (value.isEmpty()) return null
    return parseDecimalInput(value)
}

private fun valueToInput(value: Double): String {
    return if (value <= 0.0) "" else formatMacro(value)
}
