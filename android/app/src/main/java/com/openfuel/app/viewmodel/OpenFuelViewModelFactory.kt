package com.openfuel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.createSavedStateHandle
import com.openfuel.app.AppContainer

class OpenFuelViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return when (modelClass) {
            HomeViewModel::class.java -> HomeViewModel(
                logRepository = container.logRepository,
                goalsRepository = container.goalsRepository,
                savedStateHandle = extras.createSavedStateHandle(),
            )
            HistoryViewModel::class.java -> HistoryViewModel(
                logRepository = container.logRepository,
            )
            AddFoodViewModel::class.java -> AddFoodViewModel(
                foodRepository = container.foodRepository,
                logRepository = container.logRepository,
                settingsRepository = container.settingsRepository,
                remoteFoodDataSource = container.remoteFoodDataSource,
                userInitiatedNetworkGuard = container.networkGuard,
            )
            FoodLibraryViewModel::class.java -> FoodLibraryViewModel(
                foodRepository = container.foodRepository,
            )
            ScanBarcodeViewModel::class.java -> ScanBarcodeViewModel(
                remoteFoodDataSource = container.remoteFoodDataSource,
                userInitiatedNetworkGuard = container.networkGuard,
                foodRepository = container.foodRepository,
                logRepository = container.logRepository,
                settingsRepository = container.settingsRepository,
            )
            SettingsViewModel::class.java -> SettingsViewModel(
                settingsRepository = container.settingsRepository,
                entitlementsRepository = container.entitlementsRepository,
                goalsRepository = container.goalsRepository,
                exportManager = container.exportManager,
            )
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        } as T
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return create(modelClass, CreationExtras.Empty)
    }
}
