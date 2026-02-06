package com.openfuel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.openfuel.app.AppContainer

class OpenFuelViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            HomeViewModel::class.java -> HomeViewModel(
                logRepository = container.logRepository,
                goalsRepository = container.goalsRepository,
            )
            AddFoodViewModel::class.java -> AddFoodViewModel(
                foodRepository = container.foodRepository,
                logRepository = container.logRepository,
                remoteFoodDataSource = container.remoteFoodDataSource,
                userInitiatedNetworkGuard = container.networkGuard,
            )
            FoodLibraryViewModel::class.java -> FoodLibraryViewModel(
                foodRepository = container.foodRepository,
            )
            SettingsViewModel::class.java -> SettingsViewModel(
                settingsRepository = container.settingsRepository,
                goalsRepository = container.goalsRepository,
                exportManager = container.exportManager,
            )
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        } as T
    }
}
