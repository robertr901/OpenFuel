package com.openfuel.app.ui.screens

import android.content.ClipData
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openfuel.app.BuildConfig
import com.openfuel.app.domain.service.FoodCatalogProviderDescriptor
import com.openfuel.app.domain.model.DailyGoal
import com.openfuel.app.domain.util.GoalValidation
import com.openfuel.app.export.ExportFormat
import com.openfuel.app.ui.components.OFCard
import com.openfuel.app.ui.components.OFPrimaryButton
import com.openfuel.app.ui.components.OFRow
import com.openfuel.app.ui.components.OFSectionHeader
import com.openfuel.app.ui.components.OFSecondaryButton
import com.openfuel.app.ui.components.ProPaywallDialog
import com.openfuel.app.ui.theme.Dimens
import com.openfuel.app.ui.util.formatMacro
import com.openfuel.app.ui.util.parseDecimalInput
import com.openfuel.app.viewmodel.AdvancedExportState
import com.openfuel.app.viewmodel.ExportState
import com.openfuel.app.viewmodel.GoalSaveResult
import com.openfuel.app.viewmodel.SettingsViewModel
import java.util.Locale

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
    var isDiagnosticsExpanded by rememberSaveable { mutableStateOf(false) }

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

    LaunchedEffect(uiState.advancedExportState) {
        val advancedExportState = uiState.advancedExportState
        if (advancedExportState is AdvancedExportState.Success) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                advancedExportState.file,
            )
            val mimeType = if (advancedExportState.file.extension.equals("csv", ignoreCase = true)) {
                "text/csv"
            } else {
                "application/json"
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newUri(context.contentResolver, "OpenFuel export", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share export"))
            viewModel.consumeAdvancedExport()
        } else if (advancedExportState is AdvancedExportState.Error) {
            snackbarHostState.showSnackbar(advancedExportState.message)
        }
    }

    LaunchedEffect(uiState.entitlementActionMessage) {
        val message = uiState.entitlementActionMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeEntitlementMessage()
    }

    if (showGoalsDialog) {
        GoalsDialog(
            currentGoal = uiState.dailyGoal,
            onDismiss = { showGoalsDialog = false },
            onSave = { calories, protein, carbs, fat ->
                viewModel.saveGoals(calories, protein, carbs, fat)
            },
        )
    }

    Scaffold(
        modifier = Modifier.testTag("screen_settings"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
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
                .padding(Dimens.m)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.m),
        ) {
            OFCard(modifier = Modifier.fillMaxWidth()) {
                OFSectionHeader(
                    title = "Privacy",
                    subtitle = "Your logs stay on device. Online lookup is optional.",
                    modifier = Modifier.semantics { heading() },
                )
                OFRow(
                    title = "Online food lookup",
                    subtitle = "Enabled by default. Turn off anytime for fully offline use.",
                    trailing = {
                        Switch(
                            checked = uiState.onlineLookupEnabled,
                            onCheckedChange = { viewModel.setOnlineLookupEnabled(it) },
                            modifier = Modifier
                                .testTag("settings_online_lookup_switch")
                                .semantics {
                                    contentDescription = "Online food lookup toggle"
                                    stateDescription = if (uiState.onlineLookupEnabled) {
                                        "On"
                                    } else {
                                        "Off"
                                    }
                                },
                        )
                    },
                )
            }
            val onlineProviderSetup = uiState.providerDiagnostics.filterNot { provider ->
                provider.key == "static_sample" || provider.key.endsWith("_stub")
            }
            if (onlineProviderSetup.isNotEmpty()) {
                OFCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_online_provider_setup_section"),
                ) {
                    OFSectionHeader(
                        title = "Online provider setup",
                        subtitle = "Local status only. Secrets are never displayed.",
                        modifier = Modifier.semantics { heading() },
                    )
                    onlineProviderSetup.forEach { provider ->
                        val setupState = providerSetupState(provider)
                        OFRow(
                            title = provider.displayName,
                            subtitle = "$setupState · ${provider.statusReason}",
                            contentDescription = "${provider.displayName}. $setupState. ${provider.statusReason}",
                            testTag = "settings_online_provider_setup_row_${provider.key}",
                        )
                    }
                }
            }
            if (uiState.showDebugProToggle) {
                OFCard(modifier = Modifier.fillMaxWidth()) {
                    OFSectionHeader(
                        title = "Developer",
                        subtitle = "Debug-only controls and local diagnostics.",
                        trailing = {
                            TextButton(
                                onClick = { isDiagnosticsExpanded = !isDiagnosticsExpanded },
                            ) {
                                Text(if (isDiagnosticsExpanded) "Hide diagnostics" else "Show diagnostics")
                            }
                        },
                    )
                    OFRow(
                        title = "Enable Pro (debug)",
                        subtitle = "Debug-only entitlement override.",
                        trailing = {
                            Switch(
                                checked = uiState.isPro,
                                onCheckedChange = { viewModel.setProEnabled(it) },
                            )
                        },
                    )
                    if (uiState.showSecurityWarning) {
                        Text(
                            text = "Security note: running on emulator/test-keys. Treat Pro behavior as non-production.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    AnimatedVisibility(
                        visible = isDiagnosticsExpanded,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(Dimens.s),
                        ) {
                            OFSectionHeader(
                                title = "Provider diagnostics",
                                subtitle = "Local-only execution and capability metadata.",
                            )
                            uiState.providerDiagnostics.forEach { provider ->
                                val capabilities = buildList {
                                    if (provider.supportsTextSearch) add("Text search")
                                    if (provider.supportsBarcode) add("Barcode")
                                }.ifEmpty { listOf("No capabilities") }
                                OFRow(
                                    title = "${provider.displayName}: ${if (provider.enabled) "Enabled" else "Disabled"}",
                                    subtitle = "${provider.statusReason} · ${capabilities.joinToString()}",
                                )
                            }
                            val lastExecution = uiState.lastProviderExecution
                            if (lastExecution != null) {
                                OFSectionHeader(title = "Last execution")
                                Text(
                                    text = "Elapsed: ${lastExecution.report.overallElapsedMs} ms",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "Cache: ${lastExecution.report.cacheStats.hitCount} hit(s), ${lastExecution.report.cacheStats.missCount} miss(es)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                lastExecution.report.providerResults.forEach { result ->
                                    val count = result.items.size
                                    val status = result.status.name.lowercase(Locale.ROOT).replace('_', ' ')
                                    Text(
                                        text = "${result.providerId}: $status · ${result.elapsedMs} ms · $count item(s)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            OFCard(modifier = Modifier.fillMaxWidth()) {
                OFSectionHeader(
                    title = "Goals",
                    subtitle = "Optional targets stored locally on this device and applied to all days.",
                )
                GoalSummary(goal = uiState.dailyGoal)
                OFSecondaryButton(
                    text = "Edit goals",
                    onClick = { showGoalsDialog = true },
                )
            }

            OFCard(modifier = Modifier.fillMaxWidth()) {
                OFSectionHeader(
                    title = "Export",
                    subtitle = "Export all data to a JSON file you can store or share.",
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
                        OFPrimaryButton(
                            text = "Retry export",
                            onClick = {
                                viewModel.exportData(context.cacheDir, BuildConfig.VERSION_NAME)
                            },
                        )
                    }
                    else -> {
                        OFPrimaryButton(
                            text = "Export data",
                            onClick = {
                                viewModel.exportData(context.cacheDir, BuildConfig.VERSION_NAME)
                            },
                        )
                    }
                }
            }

            OFCard(modifier = Modifier.fillMaxWidth()) {
                OFSectionHeader(
                    title = "Advanced Export (Pro)",
                )
                if (uiState.isPro) {
                    Text(
                        text = "Advanced export is enabled for your account.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Review before sharing. Redacted mode removes brand fields from exported foods.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.s),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            modifier = Modifier.testTag("settings_advanced_export_format_json"),
                            onClick = { viewModel.setAdvancedExportFormat(ExportFormat.JSON) },
                        ) {
                            val selected = uiState.advancedExportFormat == ExportFormat.JSON
                            Text(if (selected) "JSON ✓" else "JSON")
                        }
                        TextButton(
                            modifier = Modifier.testTag("settings_advanced_export_format_csv"),
                            onClick = { viewModel.setAdvancedExportFormat(ExportFormat.CSV) },
                        ) {
                            val selected = uiState.advancedExportFormat == ExportFormat.CSV
                            Text(if (selected) "CSV ✓" else "CSV")
                        }
                    }
                    OFRow(
                        title = "Redacted export",
                        subtitle = "Hide brand names in exported files.",
                        trailing = {
                            Switch(
                                modifier = Modifier.testTag("settings_advanced_export_redacted_toggle"),
                                checked = uiState.advancedExportRedacted,
                                onCheckedChange = viewModel::setAdvancedExportRedacted,
                            )
                        },
                    )
                    Text(
                        text = "Preview: ${uiState.advancedExportPreview.foodCount} foods, " +
                            "${uiState.advancedExportPreview.mealEntryCount} entries, " +
                            "${uiState.advancedExportPreview.dailyGoalCount} goals, " +
                            "${uiState.advancedExportPreview.redactedBrandCount} brand field(s) redacted.",
                        modifier = Modifier.testTag("settings_advanced_export_preview"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val advancedState = uiState.advancedExportState
                    when (advancedState) {
                        is AdvancedExportState.Exporting -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        is AdvancedExportState.Error -> {
                            Text(
                                text = advancedState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                            OFPrimaryButton(
                                text = "Retry advanced export",
                                onClick = {
                                    viewModel.exportAdvancedData(context.cacheDir, BuildConfig.VERSION_NAME)
                                },
                                testTag = "settings_advanced_export_button",
                            )
                        }
                        else -> {
                            OFPrimaryButton(
                                text = "Export advanced file",
                                onClick = {
                                    viewModel.exportAdvancedData(context.cacheDir, BuildConfig.VERSION_NAME)
                                },
                                testTag = "settings_advanced_export_button",
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Upgrade to Pro to unlock advanced export formats.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OFPrimaryButton(
                        text = "See Pro options",
                        onClick = viewModel::openPaywall,
                        testTag = "settings_open_paywall_button",
                    )
                }
            }
            Spacer(modifier = Modifier.height(Dimens.xl))
        }

        ProPaywallDialog(
            show = uiState.showPaywall,
            isActionInProgress = uiState.isEntitlementActionInProgress,
            message = uiState.entitlementActionMessage,
            onDismiss = viewModel::dismissPaywall,
            onPurchaseClick = viewModel::purchasePro,
            onRestoreClick = viewModel::restorePurchases,
        )
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
        text = "No goals set.",
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
        title = { Text("Edit goals") },
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

private fun providerSetupState(provider: FoodCatalogProviderDescriptor): String {
    if (provider.enabled) {
        return "Configured"
    }
    val reason = provider.statusReason.lowercase(Locale.ROOT)
    return if (
        reason.contains("api key missing") ||
        reason.contains("credentials missing") ||
        reason.contains("needs setup")
    ) {
        "Needs setup"
    } else {
        "Disabled"
    }
}
