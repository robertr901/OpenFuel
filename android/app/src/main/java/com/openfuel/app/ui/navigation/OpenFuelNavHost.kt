package com.openfuel.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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
import com.openfuel.app.ui.screens.HomeScreen
import com.openfuel.app.ui.screens.SettingsScreen
import com.openfuel.app.viewmodel.AddFoodViewModel
import com.openfuel.app.viewmodel.FoodLibraryViewModel
import com.openfuel.app.viewmodel.HomeViewModel
import com.openfuel.app.viewmodel.OpenFuelViewModelFactory
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
                route = Routes.SETTINGS,
                label = "Settings",
                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings tab") },
            ),
        )
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in Routes.topLevelRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                OpenFuelBottomNavigation(
                    currentRoute = currentRoute,
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
            composable(Routes.TODAY) {
                val viewModel: HomeViewModel = viewModel(factory = viewModelFactory)
                HomeScreen(
                    viewModel = viewModel,
                    onAddFood = { navController.navigate(Routes.ADD_FOOD) },
                    onOpenSettings = { navController.navigateToTopLevel(Routes.SETTINGS) },
                    onOpenFoodDetail = { foodId -> navController.navigate(Routes.foodDetailRoute(foodId)) },
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
            composable(Routes.ADD_FOOD) {
                val viewModel: AddFoodViewModel = viewModel(factory = viewModelFactory)
                AddFoodScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenFoodDetail = { foodId -> navController.navigate(Routes.foodDetailRoute(foodId)) },
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

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit,
)
