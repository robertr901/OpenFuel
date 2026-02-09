package com.openfuel.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openfuel.app.BuildConfig
import com.openfuel.app.domain.intelligence.FoodTextItem
import com.openfuel.app.domain.intelligence.IntelligenceService
import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.MealType
import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.domain.model.RemoteFoodSource
import com.openfuel.app.domain.search.SearchSourceFilter
import com.openfuel.app.domain.service.ProviderStatus
import com.openfuel.app.domain.voice.VoiceTranscribeConfig
import com.openfuel.app.domain.voice.VoiceTranscribeResult
import com.openfuel.app.domain.voice.VoiceTranscriber
import com.openfuel.app.domain.voice.messageOrNull
import com.openfuel.app.ui.components.MealTypeDropdown
import com.openfuel.app.ui.components.OFCard
import com.openfuel.app.ui.components.OFEmptyState
import com.openfuel.app.ui.components.OFPill
import com.openfuel.app.ui.components.OFPrimaryButton
import com.openfuel.app.ui.components.OFRow
import com.openfuel.app.ui.components.OFSectionHeader
import com.openfuel.app.ui.components.OFSecondaryButton
import com.openfuel.app.ui.components.UnitDropdown
import com.openfuel.app.ui.theme.Dimens
import com.openfuel.app.ui.util.formatCalories
import com.openfuel.app.ui.util.formatMacro
import com.openfuel.app.ui.util.parseDecimalInput
import com.openfuel.app.viewmodel.AddFoodViewModel
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodScreen(
    viewModel: AddFoodViewModel,
    intelligenceService: IntelligenceService,
    voiceTranscriber: VoiceTranscriber,
    onNavigateBack: () -> Unit,
    onOpenFoodDetail: (String) -> Unit,
    onScanBarcode: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var searchInput by rememberSaveable { mutableStateOf(uiState.searchQuery) }
    var isQuickAddTextDialogVisible by rememberSaveable { mutableStateOf(false) }
    var isDiagnosticsExpanded by rememberSaveable { mutableStateOf(false) }
    var quickAddTextInput by rememberSaveable { mutableStateOf("") }
    val applySearchQuery: (String) -> Unit = { newQuery ->
        searchInput = newQuery
        viewModel.updateSearchQuery(newQuery)
    }

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
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
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
                .testTag("add_food_unified_results_list")
                .padding(padding)
                .padding(horizontal = Dimens.m),
            verticalArrangement = Arrangement.spacedBy(Dimens.l),
        ) {
            item {
                QuickActionsCard(
                    onOpenQuickAdd = {
                        isQuickAddTextDialogVisible = true
                    },
                    onScanBarcode = onScanBarcode,
                )
            }
            item {
                UnifiedSearchControls(
                    query = searchInput,
                    sourceFilter = uiState.sourceFilter,
                    isOnlineSearchInProgress = uiState.isOnlineSearchInProgress,
                    onQueryChange = applySearchQuery,
                    onSourceFilterChange = viewModel::setSourceFilter,
                    onSearchOnline = viewModel::searchOnline,
                    onRefreshOnline = viewModel::refreshOnline,
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
                val resultSections = buildUnifiedSearchSections(uiState.sourceFilter)
                resultSections.forEach { section ->
                    item {
                        OFSectionHeader(
                            title = section.title,
                            subtitle = section.subtitle,
                            modifier = Modifier.testTag(section.headerTestTag),
                        )
                    }
                    when (section.type) {
                        UnifiedSearchSectionType.LOCAL -> {
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

                        UnifiedSearchSectionType.ONLINE -> {
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
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(Dimens.s),
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center,
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.testTag("add_food_unified_online_loading"),
                                            )
                                        }
                                        Text(
                                            text = "Searching online catalogs...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.testTag("add_food_unified_online_loading_text"),
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
                                        modifier = Modifier.testTag("add_food_unified_online_result_${food.sourceId}"),
                                    )
                                }
                            }

                            val failedStatuses = setOf(
                                ProviderStatus.NETWORK_UNAVAILABLE,
                                ProviderStatus.HTTP_ERROR,
                                ProviderStatus.PARSING_ERROR,
                                ProviderStatus.ERROR,
                                ProviderStatus.TIMEOUT,
                                ProviderStatus.GUARD_REJECTED,
                                ProviderStatus.RATE_LIMITED,
                            )
                            val failedProviders = uiState.onlineProviderResults.filter { result ->
                                result.status in failedStatuses
                            }
                            if (BuildConfig.DEBUG && uiState.onlineProviderResults.isNotEmpty()) {
                                item {
                                    OFCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("add_food_unified_provider_debug"),
                                    ) {
                                        OFSectionHeader(
                                            title = "Provider diagnostics",
                                            subtitle = "Debug-only local execution details.",
                                            trailing = {
                                                TextButton(
                                                    onClick = {
                                                        isDiagnosticsExpanded = !isDiagnosticsExpanded
                                                    },
                                                    modifier = Modifier.testTag("add_food_unified_provider_debug_toggle"),
                                                ) {
                                                    Text(
                                                        text = if (isDiagnosticsExpanded) "Hide advanced" else "Show advanced",
                                                    )
                                                }
                                            },
                                        )
                                        Text(
                                            text = "Execution #${uiState.onlineExecutionCount}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.testTag("add_food_unified_provider_debug_execution_count"),
                                        )
                                        Text(
                                            text = "Elapsed ${uiState.onlineExecutionElapsedMs} ms · cache ${uiState.onlineProviderResults.count { it.fromCache }} hit(s)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        AnimatedVisibility(
                                            visible = isDiagnosticsExpanded,
                                            enter = fadeIn(),
                                            exit = fadeOut(),
                                        ) {
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(Dimens.s),
                                            ) {
                                                if (uiState.onlineProviderResults.any { it.fromCache }) {
                                                    Text(
                                                        text = "Cache hit",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.testTag("add_food_unified_debug_cache_hit"),
                                                    )
                                                }
                                                uiState.onlineProviderResults.forEach { result ->
                                                    Text(
                                                        text = "${result.providerId}: ${result.status.name} · ${result.elapsedMs} ms · ${result.items.size} item(s)",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = if (result.status in failedStatuses) {
                                                            MaterialTheme.colorScheme.error
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                        },
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (failedProviders.isNotEmpty() && uiState.onlineErrorMessage == null) {
                                item {
                                    Text(
                                        text = "Some providers were unavailable. Showing available results.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(Dimens.xl))
            }
        }
    }

    if (isQuickAddTextDialogVisible) {
        QuickAddTextDialog(
            input = quickAddTextInput,
            intelligenceService = intelligenceService,
            voiceTranscriber = voiceTranscriber,
            onInputChange = { quickAddTextInput = it },
            onQuickAdd = { input ->
                handleQuickAdd(
                    input = input,
                    viewModel = viewModel,
                    scope = scope,
                    snackbarHostState = snackbarHostState,
                ).also { didLog ->
                    if (didLog) {
                        isQuickAddTextDialogVisible = false
                    }
                }
            },
            onDismiss = { isQuickAddTextDialogVisible = false },
            onSelectItem = { candidate ->
                val normalized = intelligenceService.normaliseSearchQuery(candidate)
                if (normalized.isNotBlank()) {
                    applySearchQuery(normalized)
                }
                isQuickAddTextDialogVisible = false
            },
        )
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
    onRefreshOnline: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    OFCard {
        OFSectionHeader(
            title = "Local search",
            subtitle = "Search foods already saved on this device.",
        )
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Search foods") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() },
            ),
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
            horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
        ) {
            OFPrimaryButton(
                text = "Search online",
                onClick = onSearchOnline,
                enabled = query.isNotBlank() && !isOnlineSearchInProgress,
                modifier = Modifier.weight(1f),
                testTag = "add_food_unified_search_online",
            )
            OFSecondaryButton(
                text = "Refresh online",
                onClick = onRefreshOnline,
                enabled = query.isNotBlank() && !isOnlineSearchInProgress,
                modifier = Modifier.weight(1f),
                testTag = "add_food_unified_refresh_online",
            )
        }
    }
}

@Composable
private fun QuickAddTextDialog(
    input: String,
    intelligenceService: IntelligenceService,
    voiceTranscriber: VoiceTranscriber,
    onInputChange: (String) -> Unit,
    onQuickAdd: (QuickAddInput) -> Boolean,
    onDismiss: () -> Unit,
    onSelectItem: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var voiceUiState by remember { mutableStateOf<QuickAddVoiceUiState>(QuickAddVoiceUiState.Idle) }
    var voiceJob by remember { mutableStateOf<Job?>(null) }
    var isManualDetailsExpanded by rememberSaveable { mutableStateOf(false) }
    val intent = intelligenceService.parseFoodText(input)
    val dismissDialog = {
        voiceJob?.cancel()
        voiceUiState = QuickAddVoiceUiState.Idle
        onDismiss()
    }
    val startVoiceCapture = {
        voiceJob?.cancel()
        voiceUiState = QuickAddVoiceUiState.Listening
        voiceJob = scope.launch {
            try {
                val result = voiceTranscriber.transcribeOnce(
                    config = VoiceTranscribeConfig(
                        languageTag = Locale.getDefault().toLanguageTag(),
                        maxDurationMs = 10_000L,
                    ),
                )
                voiceUiState = when (result) {
                    is VoiceTranscribeResult.Success -> {
                        onInputChange(result.text)
                        QuickAddVoiceUiState.Result("Voice text ready. Review before adding.")
                    }
                    VoiceTranscribeResult.Cancelled -> QuickAddVoiceUiState.Idle
                    is VoiceTranscribeResult.Unavailable -> QuickAddVoiceUiState.Unavailable(
                        result.messageOrNull() ?: "Voice input unavailable on this device.",
                    )
                    is VoiceTranscribeResult.Failure -> QuickAddVoiceUiState.Error(
                        result.messageOrNull() ?: "Voice input failed. Please try again.",
                    )
                }
            } catch (_: CancellationException) {
                voiceUiState = QuickAddVoiceUiState.Idle
            }
        }
    }
    AlertDialog(
        onDismissRequest = dismissDialog,
        title = {
            Text("Quick add")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Dimens.sm),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Helper only. Review before adding.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        onInputChange(it)
                        if (voiceUiState is QuickAddVoiceUiState.Result) {
                            voiceUiState = QuickAddVoiceUiState.Idle
                        }
                    },
                    label = { Text("Paste text") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() },
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = startVoiceCapture,
                            enabled = voiceUiState !is QuickAddVoiceUiState.Listening,
                            modifier = Modifier.testTag("add_food_quick_add_voice_button"),
                        ) {
                            if (voiceUiState is QuickAddVoiceUiState.Listening) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(Dimens.l),
                                    strokeWidth = Dimens.xxs,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Mic,
                                    contentDescription = "Start voice input",
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_food_quick_add_text_input"),
                )
                if (voiceUiState is QuickAddVoiceUiState.Listening) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = "Voice input listening"
                                stateDescription = "Listening"
                                liveRegion = LiveRegionMode.Polite
                            }
                            .testTag("add_food_quick_add_voice_listening"),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Dimens.l),
                            strokeWidth = Dimens.xxs,
                        )
                        Text(
                            text = "Listening...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = {
                                voiceJob?.cancel()
                                voiceUiState = QuickAddVoiceUiState.Idle
                            },
                            modifier = Modifier.testTag("add_food_quick_add_voice_cancel"),
                        ) {
                            Text("Cancel")
                        }
                    }
                }
                if (voiceUiState is QuickAddVoiceUiState.Result) {
                    Text(
                        text = (voiceUiState as QuickAddVoiceUiState.Result).message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (voiceUiState is QuickAddVoiceUiState.Unavailable) {
                    Text(
                        text = (voiceUiState as QuickAddVoiceUiState.Unavailable).message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("add_food_quick_add_voice_error"),
                    )
                }
                if (voiceUiState is QuickAddVoiceUiState.Error) {
                    Text(
                        text = (voiceUiState as QuickAddVoiceUiState.Error).message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("add_food_quick_add_voice_error"),
                    )
                }
                if (intent.items.isEmpty()) {
                    Text(
                        text = "Try: 2 eggs and banana",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Dimens.s),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Parsed quick add preview list" }
                            .testTag("add_food_quick_add_text_preview_list"),
                    ) {
                        intent.items.forEachIndexed { index, item ->
                            val label = quickAddPreviewLabel(item)
                            OFSecondaryButton(
                                text = label,
                                onClick = {
                                    onSelectItem(item.normalisedName.ifBlank { item.rawName })
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics {
                                        contentDescription = "Use parsed item ${index + 1}: $label"
                                    }
                                    .testTag("add_food_quick_add_text_preview_item_$index"),
                            )
                        }
                    }
                }
                OFSecondaryButton(
                    text = if (isManualDetailsExpanded) "Hide manual details" else "Manual details",
                    onClick = { isManualDetailsExpanded = !isManualDetailsExpanded },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            stateDescription = if (isManualDetailsExpanded) "Expanded" else "Collapsed"
                        }
                        .testTag("add_food_quick_manual_toggle"),
                )
                AnimatedVisibility(
                    visible = isManualDetailsExpanded,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    QuickAddManualForm(onQuickAdd = onQuickAdd)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = dismissDialog) {
                Text("Close")
            }
        },
    )
}

private sealed interface QuickAddVoiceUiState {
    data object Idle : QuickAddVoiceUiState

    data object Listening : QuickAddVoiceUiState

    data class Result(val message: String) : QuickAddVoiceUiState

    data class Unavailable(val message: String) : QuickAddVoiceUiState

    data class Error(val message: String) : QuickAddVoiceUiState
}

private fun quickAddPreviewLabel(item: FoodTextItem): String {
    val name = item.normalisedName.ifBlank { item.rawName.trim() }
    val quantityText = item.quantity?.toString()
    val unitText = item.unit?.name?.lowercase()
    return when {
        quantityText != null && unitText != null -> "$name ($quantityText $unitText)"
        quantityText != null -> "$name ($quantityText)"
        unitText != null -> "$name ($unitText)"
        else -> name
    }
}

@Composable
private fun QuickActionsCard(
    onOpenQuickAdd: () -> Unit,
    onScanBarcode: () -> Unit,
) {
    OFCard {
        OFSectionHeader(
            title = "Quick actions",
            subtitle = "Scan or launch quick add with explicit actions.",
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
        ) {
            OFSecondaryButton(
                text = "Scan barcode",
                onClick = onScanBarcode,
                modifier = Modifier.weight(1f),
                testTag = "add_food_unified_scan_barcode",
            )
            OFSecondaryButton(
                text = "Quick add",
                onClick = onOpenQuickAdd,
                modifier = Modifier.weight(1f),
                testTag = "add_food_quick_add_text_button",
            )
        }
    }
}

@Composable
private fun QuickAddManualForm(
    onQuickAdd: (QuickAddInput) -> Boolean,
) {
    val focusManager = LocalFocusManager.current
    var name by rememberSaveable { mutableStateOf("") }
    var calories by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("") }
    var carbs by rememberSaveable { mutableStateOf("") }
    var fat by rememberSaveable { mutableStateOf("") }
    var mealType by rememberSaveable { mutableStateOf(MealType.BREAKFAST) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.sm),
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Food name") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_food_quick_name_input"),
        )
        OutlinedTextField(
            value = calories,
            onValueChange = { calories = it },
            label = { Text("Calories (kcal)") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_food_quick_calories_input"),
        )
        OutlinedTextField(
            value = protein,
            onValueChange = { protein = it },
            label = { Text("Protein (g)") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_food_quick_protein_input"),
        )
        OutlinedTextField(
            value = carbs,
            onValueChange = { carbs = it },
            label = { Text("Carbs (g)") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_food_quick_carbs_input"),
        )
        OutlinedTextField(
            value = fat,
            onValueChange = { fat = it },
            label = { Text("Fat (g)") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() },
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_food_quick_fat_input"),
        )
        MealTypeDropdown(
            selected = mealType,
            onSelected = { mealType = it },
            modifier = Modifier.fillMaxWidth(),
        )
        OFPrimaryButton(
            text = "Log quick add",
            onClick = {
                val didLog = onQuickAdd(
                    QuickAddInput(
                        name = name,
                        calories = calories,
                        protein = protein,
                        carbs = carbs,
                        fat = fat,
                        mealType = mealType,
                    ),
                )
                if (didLog) {
                    focusManager.clearFocus()
                    name = ""
                    calories = ""
                    protein = ""
                    carbs = ""
                    fat = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
            testTag = "add_food_quick_log_button",
        )
    }
}

private fun handleQuickAdd(
    input: QuickAddInput,
    viewModel: AddFoodViewModel,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
): Boolean {
    val maxCalories = 10_000.0
    val maxMacro = 1_000.0
    val caloriesValue = parseDecimalInput(input.calories)
    if (caloriesValue == null) {
        scope.launch { snackbarHostState.showSnackbar("Enter calories") }
        return false
    }
    val proteinValue = parseDecimalInput(input.protein) ?: 0.0
    val carbsValue = parseDecimalInput(input.carbs) ?: 0.0
    val fatValue = parseDecimalInput(input.fat) ?: 0.0
    if (caloriesValue !in 0.0..maxCalories) {
        scope.launch { snackbarHostState.showSnackbar("Calories must be between 0 and 10000.") }
        return false
    }
    if (proteinValue !in 0.0..maxMacro || carbsValue !in 0.0..maxMacro || fatValue !in 0.0..maxMacro) {
        scope.launch { snackbarHostState.showSnackbar("Macros must be between 0 and 1000 g.") }
        return false
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
    return true
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
            verticalArrangement = Arrangement.spacedBy(Dimens.sm),
        ) {
            OFRow(
                title = food.name,
                subtitle = food.brand?.takeIf { it.isNotBlank() },
                trailing = {
                    OFPill(text = sourceLabel)
                },
            )
            Text(
                text = "${formatCalories(food.caloriesKcal)} kcal · ${formatMacro(food.proteinG)}p ${formatMacro(food.carbsG)}c ${formatMacro(food.fatG)}f",
                style = instrumentTextStyle(),
            )
            MealTypeDropdown(
                selected = selectedMeal,
                onSelected = { selectedMeal = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
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
    modifier: Modifier = Modifier,
) {
    OFCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.sm),
        ) {
            OFRow(
                title = food.name,
                subtitle = food.brand?.takeIf { it.isNotBlank() },
                trailing = {
                    OFPill(text = provenanceLabel(food))
                },
            )
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
                    style = instrumentTextStyle(),
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

private fun provenanceLabel(food: RemoteFoodCandidate): String {
    val providerKey = food.providerKey.orEmpty()
    return when {
        providerKey.equals("open_food_facts", ignoreCase = true) -> "OFF"
        providerKey.equals("static_sample", ignoreCase = true) -> "Sample"
        providerKey.isNotBlank() -> providerKey
        food.source == RemoteFoodSource.OPEN_FOOD_FACTS -> "OFF"
        food.source == RemoteFoodSource.STATIC_SAMPLE -> "Sample"
        else -> "Online"
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
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
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
                    style = instrumentTextStyle(),
                )
                Text(
                    text = "Protein: ${formatMacro(food.proteinGPer100g ?: 0.0)} g",
                    style = instrumentTextStyle(),
                )
                Text(
                    text = "Carbs: ${formatMacro(food.carbsGPer100g ?: 0.0)} g",
                    style = instrumentTextStyle(),
                )
                Text(
                    text = "Fat: ${formatMacro(food.fatGPer100g ?: 0.0)} g",
                    style = instrumentTextStyle(),
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
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
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

@Composable
private fun instrumentTextStyle() = MaterialTheme.typography.labelLarge.copy(
    fontWeight = FontWeight.Medium,
    fontFeatureSettings = "tnum",
)
