package com.openfuel.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openfuel.app.ui.components.LocalFoodResultRow
import com.openfuel.app.ui.components.OFEmptyState
import com.openfuel.app.ui.components.SectionHeader
import com.openfuel.app.ui.theme.Dimens
import com.openfuel.app.viewmodel.FoodLibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodLibraryScreen(
    viewModel: FoodLibraryViewModel,
    onAddFood: () -> Unit,
    onOpenFoodDetail: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var searchInput by rememberSaveable { mutableStateOf(uiState.searchQuery) }
    val queryBlank = searchInput.isBlank()

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
        modifier = Modifier.testTag("screen_foods"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Foods",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddFood,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add food",
                    )
                },
                text = { Text("Add food") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimens.m),
            verticalArrangement = Arrangement.spacedBy(Dimens.m),
        ) {
            OutlinedTextField(
                value = searchInput,
                onValueChange = {
                    searchInput = it
                    viewModel.updateSearchQuery(it)
                },
                label = { Text("Search foods") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("foods_query_input"),
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("foods_results_list"),
                verticalArrangement = Arrangement.spacedBy(Dimens.s),
            ) {
                if (queryBlank) {
                    item {
                        SectionHeader(
                            title = "Recent logs",
                            subtitle = "Fastest way to repeat foods you already logged.",
                            modifier = Modifier.testTag("foods_recent_section"),
                        )
                    }
                    if (uiState.recentFoods.isEmpty()) {
                        item {
                            OFEmptyState(
                                title = "No recent foods yet",
                                body = "Log a meal once and it will appear here for quick reuse.",
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = uiState.recentFoods,
                            key = { _, item -> "recent-${item.id}" },
                        ) { index, food ->
                            LocalFoodResultRow(
                                food = food,
                                sourceLabel = "Recent",
                                onLog = { mealType ->
                                    viewModel.logFood(
                                        foodId = food.id,
                                        mealType = mealType,
                                    )
                                },
                                onOpenPortion = { onOpenFoodDetail(food.id) },
                                testTagPrefix = "foods_recent_$index",
                            )
                        }
                    }

                    item {
                        SectionHeader(
                            title = "Favorites",
                            subtitle = "Foods you marked for quick reuse.",
                            modifier = Modifier.testTag("foods_favorites_section"),
                        )
                    }
                    if (uiState.favoriteFoods.isEmpty()) {
                        item {
                            OFEmptyState(
                                title = "No favorites yet",
                                body = "Mark foods as favorites in details to pin them here.",
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = uiState.favoriteFoods,
                            key = { _, item -> "favorite-${item.id}" },
                        ) { index, food ->
                            LocalFoodResultRow(
                                food = food,
                                sourceLabel = "Favorite",
                                onLog = { mealType ->
                                    viewModel.logFood(
                                        foodId = food.id,
                                        mealType = mealType,
                                    )
                                },
                                onOpenPortion = { onOpenFoodDetail(food.id) },
                                testTagPrefix = "foods_favorite_$index",
                            )
                        }
                    }
                } else {
                    item {
                        SectionHeader(
                            title = "Local results",
                            subtitle = "Matching foods already saved on this device.",
                            modifier = Modifier.testTag("foods_local_results_section"),
                        )
                    }
                    if (uiState.localResults.isEmpty()) {
                        item {
                            OFEmptyState(
                                title = "No local matches",
                                body = "Try a broader search term.",
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = uiState.localResults,
                            key = { _, item -> "local-${item.id}" },
                        ) { index, food ->
                            LocalFoodResultRow(
                                food = food,
                                sourceLabel = "Local",
                                onLog = { mealType ->
                                    viewModel.logFood(
                                        foodId = food.id,
                                        mealType = mealType,
                                    )
                                },
                                onOpenPortion = { onOpenFoodDetail(food.id) },
                                testTagPrefix = "foods_local_$index",
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(Dimens.xl)) }
            }
        }
    }
}
