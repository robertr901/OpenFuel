package com.openfuel.app

import android.content.Context
import com.openfuel.app.data.entitlement.DebugEntitlementService
import com.openfuel.app.data.entitlement.PlaceholderPlayBillingEntitlementService
import com.openfuel.app.data.datastore.settingsDataStore
import com.openfuel.app.data.db.OpenFuelDatabase
import com.openfuel.app.data.remote.OpenFoodFactsRemoteFoodDataSource
import com.openfuel.app.data.remote.RemoteFoodDataSource
import com.openfuel.app.data.remote.UserInitiatedNetworkGuard
import com.openfuel.app.data.security.LocalSecurityPostureProvider
import com.openfuel.app.data.repository.EntitlementsRepositoryImpl
import com.openfuel.app.data.repository.FoodRepositoryImpl
import com.openfuel.app.data.repository.GoalsRepositoryImpl
import com.openfuel.app.data.repository.LogRepositoryImpl
import com.openfuel.app.data.repository.SettingsRepositoryImpl
import com.openfuel.app.domain.repository.EntitlementsRepository
import com.openfuel.app.domain.repository.FoodRepository
import com.openfuel.app.domain.repository.GoalsRepository
import com.openfuel.app.domain.repository.LogRepository
import com.openfuel.app.domain.repository.SettingsRepository
import com.openfuel.app.domain.security.SecurityPostureProvider
import com.openfuel.app.domain.service.EntitlementService
import com.openfuel.app.export.ExportManager
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    private val database: OpenFuelDatabase = OpenFuelDatabase.build(context)
    private val userInitiatedNetworkGuard = UserInitiatedNetworkGuard()
    private val onlineHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()

    val foodRepository: FoodRepository = FoodRepositoryImpl(database.foodDao())
    val logRepository: LogRepository = LogRepositoryImpl(
        mealEntryDao = database.mealEntryDao(),
        foodDao = database.foodDao(),
    )
    val goalsRepository: GoalsRepository = GoalsRepositoryImpl(context.settingsDataStore)
    val settingsRepository: SettingsRepository = SettingsRepositoryImpl(context.settingsDataStore)
    val entitlementsRepository: EntitlementsRepository = EntitlementsRepositoryImpl(context.settingsDataStore)
    val securityPostureProvider: SecurityPostureProvider = LocalSecurityPostureProvider(context)
    val entitlementService: EntitlementService = if (BuildConfig.DEBUG) {
        DebugEntitlementService(entitlementsRepository, securityPostureProvider)
    } else {
        PlaceholderPlayBillingEntitlementService(securityPostureProvider)
    }
    val remoteFoodDataSource: RemoteFoodDataSource = OpenFoodFactsRemoteFoodDataSource.create(
        okHttpClient = onlineHttpClient,
        userInitiatedNetworkGuard = userInitiatedNetworkGuard,
    )
    val networkGuard: UserInitiatedNetworkGuard = userInitiatedNetworkGuard

    val exportManager: ExportManager = ExportManager(
        foodDao = database.foodDao(),
        mealEntryDao = database.mealEntryDao(),
        goalsRepository = goalsRepository,
    )
}
