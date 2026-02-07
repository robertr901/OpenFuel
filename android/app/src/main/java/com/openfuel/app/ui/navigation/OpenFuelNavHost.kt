package com.openfuel.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.openfuel.app.OpenFuelApp
import com.openfuel.app.ui.screens.AddFoodScreen
import com.openfuel.app.ui.screens.FoodDetailScreen
import com.openfuel.app.ui.screens.FoodLibraryScreen
import com.openfuel.app.ui.screens.HistoryScreen
import com.openfuel.app.ui.screens.HomeScreen
import com.openfuel.app.ui.screens.InsightsScreen
import com.openfuel.app.ui.screens.ScanBarcodeScreen
import com.openfuel.app.ui.screens.SettingsScreen
import com.openfuel.app.viewmodel.AddFoodViewModel
import com.openfuel.app.viewmodel.FoodLibraryViewModel
import com.openfuel.app.viewmodel.HistoryViewModel
import com.openfuel.app.viewmodel.HomeViewModel
import com.openfuel.app.viewmodel.InsightsViewModel
import com.openfuel.app.viewmodel.OpenFuelViewModelFactory
import com.openfuel.app.viewmodel.ScanBarcodeViewModel
import com.openfuel.app.viewmodel.SettingsViewModel

@Composable
fun OpenFuelAppRoot() {
    val context = LocalContext.current
    val application = context.applicationContext as OpenFuelApp
    val container = application.container
    val navController = rememberNavController()
    val viewModelFactory = remember { OpenFuelViewModelFactory(container) }
    val topLevelDestinations = remember {
        listOf(
            TopLevelDestination(
                route = Routes.TODAY,
                label = "Today",
                icon = { Icon(Icons.Default.Home, contentDescription = "Today tab") },
            ),
            TopLevelDestination(
                route = Routes.HISTORY,
                label = "History",
                icon = { Icon(Icons.Default.History, contentDescription = "History tab") },
            ),
            TopLevelDestination(
                route = Routes.FOODS,
                label = "Foods",
                icon = {
                    Icon(
                        imageVector = Icons.Default.RestaurantMenu,
                        contentDescription = "Foods tab",
                    )
                },
            ),
            TopLevelDestination(
                route = Routes.INSIGHTS,
                label = "Insights",
                icon = { Icon(Icons.Default.Insights, contentDescription = "Insights tab") },
            ),
            TopLevelDestination(
                route = Routes.SETTINGS,
                label = "Settings",
                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings tab") },
            ),
        )
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val destinationRoute = navBackStackEntry?.destination?.route
    val currentTopLevelRoute = destinationRoute?.routeBase()
    val showBottomBar = currentTopLevelRoute in Routes.topLevelRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                OpenFuelBottomNavigation(
                    currentRoute = currentTopLevelRoute,
                    destinations = topLevelDestinations,
                    onDestinationSelected = { route ->
                        navController.navigateToTopLevel(route)
                    },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.TODAY,
            modifier = Modifier.padding(padding),
        ) {
            composable(
                route = Routes.TODAY_ROUTE,
                arguments = listOf(
                    navArgument(Routes.SELECTED_DATE_ARG) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                val viewModel: HomeViewModel = viewModel(factory = viewModelFactory)
                LaunchedEffect(entry.arguments?.getString(Routes.SELECTED_DATE_ARG)) {
                    viewModel.applyNavigationDate(entry.arguments?.getString(Routes.SELECTED_DATE_ARG))
                }
                HomeScreen(
                    viewModel = viewModel,
                    onAddFood = { navController.navigate(Routes.ADD_FOOD) },
                    onOpenSettings = { navController.navigateToTopLevel(Routes.SETTINGS) },
                    onOpenFoodDetail = { foodId -> navController.navigate(Routes.foodDetailRoute(foodId)) },
                )
            }
            composable(Routes.HISTORY) {
                val viewModel: HistoryViewModel = viewModel(factory = viewModelFactory)
                HistoryScreen(
                    viewModel = viewModel,
                    onSelectDay = { date ->
                        navController.navigateToTopLevel(Routes.todayRoute(date.toString()))
                    },
                )
            }
            composable(Routes.FOODS) {
                val viewModel: FoodLibraryViewModel = viewModel(factory = viewModelFactory)
                FoodLibraryScreen(
                    viewModel = viewModel,
                    onAddFood = { navController.navigate(Routes.ADD_FOOD) },
                    onOpenFoodDetail = { foodId -> navController.navigate(Routes.foodDetailRoute(foodId)) },
                )
            }
            composable(Routes.INSIGHTS) {
                val viewModel: InsightsViewModel = viewModel(factory = viewModelFactory)
                InsightsScreen(viewModel = viewModel)
            }
            composable(Routes.ADD_FOOD) {
                val viewModel: AddFoodViewModel = viewModel(factory = viewModelFactory)
                AddFoodScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenFoodDetail = { foodId -> navController.navigate(Routes.foodDetailRoute(foodId)) },
                    onScanBarcode = { navController.navigate(Routes.SCAN_BARCODE) },
                )
            }
            composable(Routes.SCAN_BARCODE) {
                val viewModel: ScanBarcodeViewModel = viewModel(factory = viewModelFactory)
                ScanBarcodeScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "${Routes.FOOD_DETAIL}/{foodId}",
                arguments = listOf(navArgument("foodId") { type = NavType.StringType }),
            ) { entry ->
                val foodId = entry.arguments?.getString("foodId")
                FoodDetailScreen(
                    foodId = foodId,
                    foodRepository = container.foodRepository,
                    logRepository = container.logRepository,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(Routes.SETTINGS) {
                val viewModel: SettingsViewModel = viewModel(factory = viewModelFactory)
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = null,
                )
            }
        }
    }
}

@Composable
private fun OpenFuelBottomNavigation(
    currentRoute: String?,
    destinations: List<TopLevelDestination>,
    onDestinationSelected: (String) -> Unit,
) {
    NavigationBar {
        destinations.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = { onDestinationSelected(destination.route) },
                icon = destination.icon,
                label = { Text(destination.label) },
                alwaysShowLabel = true,
            )
        }
    }
}

private fun NavHostController.navigateToTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

private fun String.routeBase(): String {
    return substringBefore('?').substringBefore('/')
}

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit,
)
