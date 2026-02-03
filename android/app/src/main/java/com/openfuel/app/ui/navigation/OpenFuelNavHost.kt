package com.openfuel.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.openfuel.app.OpenFuelApp
import com.openfuel.app.ui.screens.AddFoodScreen
import com.openfuel.app.ui.screens.FoodDetailScreen
import com.openfuel.app.ui.screens.HomeScreen
import com.openfuel.app.ui.screens.SettingsScreen
import com.openfuel.app.viewmodel.AddFoodViewModel
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

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
    ) {
        composable(Routes.HOME) {
            val viewModel: HomeViewModel = viewModel(factory = viewModelFactory)
            HomeScreen(
                viewModel = viewModel,
                onAddFood = { navController.navigate(Routes.ADD_FOOD) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
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
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
