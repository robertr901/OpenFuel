package com.openfuel.app

import android.app.Activity
import android.app.Application
import android.content.Context
import com.openfuel.app.data.entitlement.DebugEntitlementService
import com.openfuel.app.data.entitlement.PlayBillingEntitlementService
import com.openfuel.app.data.entitlement.PlayBillingGateway
import com.openfuel.app.data.datastore.settingsDataStore
import com.openfuel.app.data.db.OpenFuelDatabase
import com.openfuel.app.data.remote.DefaultFoodCatalogProviderRegistry
import com.openfuel.app.data.remote.DefaultProviderExecutor
import com.openfuel.app.data.remote.OpenFoodFactsCatalogProvider
import com.openfuel.app.data.remote.OpenFoodFactsRemoteFoodDataSource
import com.openfuel.app.data.remote.ProviderEntry
import com.openfuel.app.data.remote.RoomProviderResultCache
import com.openfuel.app.data.remote.RemoteFoodDataSource
import com.openfuel.app.data.remote.StaticSampleCatalogProvider
import com.openfuel.app.data.remote.UsdaFoodDataCentralCatalogProvider
import com.openfuel.app.data.remote.UsdaFoodDataCentralDataSource
import com.openfuel.app.data.remote.UsdaFoodDataSource
import com.openfuel.app.data.remote.UserInitiatedNetworkGuard
import com.openfuel.app.data.remote.resolveOpenFoodFactsAvailability
import com.openfuel.app.data.remote.resolveUsdaAvailability
import com.openfuel.app.data.voice.RecognizerIntentVoiceTranscriber
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
import com.openfuel.app.domain.intelligence.IntelligenceService
import com.openfuel.app.domain.intelligence.RuleBasedIntelligenceService
import com.openfuel.app.domain.voice.VoiceTranscriber
import com.openfuel.app.domain.service.EntitlementService
import com.openfuel.app.domain.service.FoodCatalogProvider
import com.openfuel.app.domain.service.FoodCatalogProviderDescriptor
import com.openfuel.app.domain.service.FoodCatalogProviderRegistry
import com.openfuel.app.domain.service.InMemoryProviderExecutionDiagnosticsStore
import com.openfuel.app.domain.service.ProviderExecutor
import com.openfuel.app.export.ExportManager
import androidx.activity.ComponentActivity
import okhttp3.OkHttpClient
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

class AppContainer(
    context: Context,
    private val forceDeterministicProvidersOnly: Boolean = false,
    private val voiceTranscriberOverride: VoiceTranscriber? = null,
) {
    private val database: OpenFuelDatabase = OpenFuelDatabase.build(context)
    private val userInitiatedNetworkGuard = UserInitiatedNetworkGuard()
    private val currentActivityRef = java.util.concurrent.atomic.AtomicReference<WeakReference<ComponentActivity>?>(null)
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
        PlayBillingEntitlementService(
            entitlementsRepository = entitlementsRepository,
            securityPostureProvider = securityPostureProvider,
            billingGateway = PlayBillingGateway(
                context = context.applicationContext,
                currentActivityProvider = { currentActivityRef.get()?.get() },
            ),
            proProductId = BuildConfig.PRO_PRODUCT_ID,
        )
    }
    val remoteFoodDataSource: RemoteFoodDataSource = OpenFoodFactsRemoteFoodDataSource.create(
        okHttpClient = onlineHttpClient,
        userInitiatedNetworkGuard = userInitiatedNetworkGuard,
    )
    private val usdaApiKey: String = BuildConfig.USDA_API_KEY.trim()
    private val openFoodFactsAvailability = resolveOpenFoodFactsAvailability(
        forceDeterministicProvidersOnly = forceDeterministicProvidersOnly,
        providerEnabledByFlag = BuildConfig.ONLINE_PROVIDER_OPEN_FOOD_FACTS_ENABLED,
    )
    private val usdaAvailability = resolveUsdaAvailability(
        forceDeterministicProvidersOnly = forceDeterministicProvidersOnly,
        providerEnabledByFlag = BuildConfig.ONLINE_PROVIDER_USDA_ENABLED,
        apiKey = usdaApiKey,
    )
    private val usdaFoodDataSource: UsdaFoodDataSource = UsdaFoodDataCentralDataSource.create(
        okHttpClient = onlineHttpClient,
        userInitiatedNetworkGuard = userInitiatedNetworkGuard,
        apiKey = usdaApiKey,
    )
    val foodCatalogProvider: FoodCatalogProvider = OpenFoodFactsCatalogProvider(
        remoteFoodDataSource = remoteFoodDataSource,
    )
    private val usdaFoodCatalogProvider: FoodCatalogProvider = UsdaFoodDataCentralCatalogProvider(
        dataSource = usdaFoodDataSource,
    )
    private val staticSampleCatalogProvider: FoodCatalogProvider = StaticSampleCatalogProvider()
    val foodCatalogProviderRegistry: FoodCatalogProviderRegistry = DefaultFoodCatalogProviderRegistry(
        entries = listOf(
            ProviderEntry(
                metadata = FoodCatalogProviderDescriptor(
                    key = "open_food_facts",
                    displayName = "Open Food Facts",
                    priority = 10,
                    supportsBarcode = true,
                    supportsTextSearch = true,
                    termsOfUseLink = "https://world.openfoodfacts.org/terms-of-use",
                    enabled = openFoodFactsAvailability.enabled,
                    statusReason = openFoodFactsAvailability.statusReason,
                ),
                provider = foodCatalogProvider,
            ),
            ProviderEntry(
                metadata = FoodCatalogProviderDescriptor(
                    key = "static_sample",
                    displayName = "Static sample (deterministic)",
                    priority = 15,
                    supportsBarcode = true,
                    supportsTextSearch = true,
                    termsOfUseLink = null,
                    enabled = true,
                    statusReason = "Deterministic debug provider.",
                ),
                provider = staticSampleCatalogProvider,
                debugDiagnosticsOnly = true,
            ),
            ProviderEntry(
                metadata = FoodCatalogProviderDescriptor(
                    key = "usda_fdc",
                    displayName = "USDA FoodData Central",
                    priority = 20,
                    supportsBarcode = true,
                    supportsTextSearch = true,
                    termsOfUseLink = "https://fdc.nal.usda.gov/api-guide/",
                    enabled = usdaAvailability.enabled,
                    statusReason = usdaAvailability.statusReason,
                ),
                provider = if (usdaAvailability.enabled) {
                    usdaFoodCatalogProvider
                } else {
                    null
                },
            ),
            ProviderEntry(
                metadata = FoodCatalogProviderDescriptor(
                    key = "nutritionix_stub",
                    displayName = "Nutritionix (stub)",
                    priority = 30,
                    supportsBarcode = true,
                    supportsTextSearch = true,
                    termsOfUseLink = "https://www.nutritionix.com/business/api",
                    enabled = false,
                    statusReason = "Disabled. API integration not implemented in this phase.",
                ),
                provider = null,
            ),
            ProviderEntry(
                metadata = FoodCatalogProviderDescriptor(
                    key = "edamam_stub",
                    displayName = "Edamam (stub)",
                    priority = 40,
                    supportsBarcode = false,
                    supportsTextSearch = true,
                    termsOfUseLink = "https://www.edamam.com/",
                    enabled = false,
                    statusReason = "Disabled. API integration not implemented in this phase.",
                ),
                provider = null,
            ),
        ),
        isDebugBuild = BuildConfig.DEBUG,
        debugDiagnosticsEnabled = BuildConfig.DEBUG,
    )
    val activeFoodCatalogProvider: FoodCatalogProvider = foodCatalogProviderRegistry.primaryTextSearchProvider()
    val providerExecutionDiagnosticsStore = InMemoryProviderExecutionDiagnosticsStore()
    val providerExecutor: ProviderExecutor = DefaultProviderExecutor(
        providerSource = { request ->
            foodCatalogProviderRegistry.providersFor(
                requestType = request.requestType,
                onlineLookupEnabled = request.onlineLookupEnabled,
            )
        },
        cache = RoomProviderResultCache(database.providerSearchCacheDao()),
        diagnosticsStore = providerExecutionDiagnosticsStore,
    )
    val networkGuard: UserInitiatedNetworkGuard = userInitiatedNetworkGuard
    val intelligenceService: IntelligenceService = RuleBasedIntelligenceService()
    val voiceTranscriber: VoiceTranscriber = voiceTranscriberOverride ?: RecognizerIntentVoiceTranscriber(
        currentActivityProvider = { currentActivityRef.get()?.get() },
    )

    val exportManager: ExportManager = ExportManager(
        foodDao = database.foodDao(),
        mealEntryDao = database.mealEntryDao(),
        goalsRepository = goalsRepository,
    )

    init {
        (context.applicationContext as? Application)?.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: android.os.Bundle?) {
                    updateCurrentActivity(activity)
                }

                override fun onActivityStarted(activity: Activity) {
                    updateCurrentActivity(activity)
                }

                override fun onActivityResumed(activity: Activity) {
                    updateCurrentActivity(activity)
                }

                override fun onActivityPaused(activity: Activity) {
                    clearCurrentActivity(activity)
                }

                override fun onActivityStopped(activity: Activity) {
                    clearCurrentActivity(activity)
                }

                override fun onActivitySaveInstanceState(
                    activity: Activity,
                    outState: android.os.Bundle,
                ) {
                }

                override fun onActivityDestroyed(activity: Activity) {
                    clearCurrentActivity(activity)
                }
            },
        )
    }

    private fun updateCurrentActivity(activity: Activity) {
        val componentActivity = activity as? ComponentActivity ?: return
        currentActivityRef.set(WeakReference(componentActivity))
    }

    private fun clearCurrentActivity(activity: Activity) {
        val componentActivity = activity as? ComponentActivity ?: return
        val current = currentActivityRef.get()?.get() ?: return
        if (current === componentActivity) {
            currentActivityRef.set(null)
        }
    }
}
