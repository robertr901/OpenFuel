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
        val viewModel: ViewModel = when (modelClass) {
            HomeViewModel::class.java -> HomeViewModel(
                logRepository = container.logRepository,
                settingsRepository = container.settingsRepository,
                goalsRepository = container.goalsRepository,
                savedStateHandle = extras.createSavedStateHandle(),
                analyticsService = container.analyticsService,
            )
            HistoryViewModel::class.java -> HistoryViewModel(
                logRepository = container.logRepository,
            )
            AddFoodViewModel::class.java -> AddFoodViewModel(
                foodRepository = container.foodRepository,
                logRepository = container.logRepository,
                settingsRepository = container.settingsRepository,
                providerExecutor = container.providerExecutor,
                userInitiatedNetworkGuard = container.networkGuard,
                analyticsService = container.analyticsService,
                onlineSearchOrchestrator = container.onlineSearchOrchestrator,
            )
            FoodLibraryViewModel::class.java -> FoodLibraryViewModel(
                foodRepository = container.foodRepository,
            )
            FoodDetailViewModel::class.java -> FoodDetailViewModel(
                foodRepository = container.foodRepository,
                logRepository = container.logRepository,
                savedStateHandle = extras.createSavedStateHandle(),
            )
            InsightsViewModel::class.java -> InsightsViewModel(
                entitlementService = container.entitlementService,
                logRepository = container.logRepository,
                paywallPromptPolicy = container.paywallPromptPolicy,
                analyticsService = container.analyticsService,
            )
            ScanBarcodeViewModel::class.java -> ScanBarcodeViewModel(
                providerExecutor = container.providerExecutor,
                userInitiatedNetworkGuard = container.networkGuard,
                foodRepository = container.foodRepository,
                logRepository = container.logRepository,
                settingsRepository = container.settingsRepository,
            )
            SettingsViewModel::class.java -> SettingsViewModel(
                settingsRepository = container.settingsRepository,
                entitlementService = container.entitlementService,
                paywallPromptPolicy = container.paywallPromptPolicy,
                analyticsService = container.analyticsService,
                goalsRepository = container.goalsRepository,
                exportManager = container.exportManager,
                foodCatalogProviderRegistry = container.foodCatalogProviderRegistry,
                providerExecutionDiagnosticsStore = container.providerExecutionDiagnosticsStore,
            )
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
        return modelClass.cast(viewModel)
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return create(modelClass, CreationExtras.Empty)
    }
}
