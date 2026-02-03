package com.openfuel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.openfuel.app.AppContainer

class OpenFuelViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            HomeViewModel::class.java -> HomeViewModel(container.logRepository)
            AddFoodViewModel::class.java -> AddFoodViewModel(
                foodRepository = container.foodRepository,
                logRepository = container.logRepository,
            )
            SettingsViewModel::class.java -> SettingsViewModel(
                settingsRepository = container.settingsRepository,
                exportManager = container.exportManager,
            )
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        } as T
    }
}
