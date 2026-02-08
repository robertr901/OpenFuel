package com.openfuel.app.data.remote

import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.domain.model.RemoteFoodSource
import com.openfuel.app.domain.service.FoodCatalogProvider
import com.openfuel.app.domain.service.FoodCatalogProviderDescriptor
import com.openfuel.app.domain.service.ProviderRequestType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultFoodCatalogProviderRegistryTest {
    @Test
    fun primaryTextSearchProvider_returnsLowestPriorityEnabledProvider() {
        val lowPriorityProvider = FakeFoodCatalogProvider(name = "low-priority")
        val highPriorityProvider = FakeFoodCatalogProvider(name = "high-priority")
        val registry = DefaultFoodCatalogProviderRegistry(
            entries = listOf(
                ProviderEntry(
                    metadata = descriptor(
                        key = "high",
                        priority = 20,
                        enabled = true,
                    ),
                    provider = highPriorityProvider,
                ),
                ProviderEntry(
                    metadata = descriptor(
                        key = "low",
                        priority = 10,
                        enabled = true,
                    ),
                    provider = lowPriorityProvider,
                ),
            ),
        )

        val selected = registry.primaryTextSearchProvider()

        assertEquals(lowPriorityProvider, selected)
    }

    @Test
    fun providerDiagnostics_exposesEnabledAndDisabledProviders() {
        val registry = DefaultFoodCatalogProviderRegistry(
            entries = listOf(
                ProviderEntry(
                    metadata = descriptor(
                        key = "open_food_facts",
                        enabled = true,
                        reason = "Active",
                    ),
                    provider = FakeFoodCatalogProvider(name = "off"),
                ),
                ProviderEntry(
                    metadata = descriptor(
                        key = "usda_stub",
                        enabled = false,
                        reason = "Not implemented",
                    ),
                    provider = null,
                ),
            ),
        )

        val diagnostics = registry.providerDiagnostics()

        assertEquals(2, diagnostics.size)
        assertTrue(diagnostics.any { it.key == "open_food_facts" && it.enabled })
        assertTrue(diagnostics.any { it.key == "usda_stub" && !it.enabled })
    }

    @Test
    fun providersFor_whenOnlineDisabled_marksProvidersDisabled() {
        val registry = DefaultFoodCatalogProviderRegistry(
            entries = listOf(
                ProviderEntry(
                    metadata = descriptor(
                        key = "open_food_facts",
                        enabled = true,
                    ),
                    provider = FakeFoodCatalogProvider(name = "off"),
                ),
            ),
        )

        val providers = registry.providersFor(
            requestType = ProviderRequestType.TEXT_SEARCH,
            onlineLookupEnabled = false,
        )

        assertEquals(1, providers.size)
        assertTrue(!providers.single().descriptor.enabled)
    }

    @Test
    fun providersFor_releaseBuildDisablesDebugOnlyProvider() {
        val registry = DefaultFoodCatalogProviderRegistry(
            entries = listOf(
                ProviderEntry(
                    metadata = descriptor(
                        key = "debug_static",
                        enabled = true,
                    ),
                    provider = FakeFoodCatalogProvider(name = "debug"),
                    debugDiagnosticsOnly = true,
                ),
            ),
            isDebugBuild = false,
            debugDiagnosticsEnabled = false,
        )

        val providers = registry.providersFor(
            requestType = ProviderRequestType.TEXT_SEARCH,
            onlineLookupEnabled = true,
        )

        assertEquals(1, providers.size)
        assertTrue(!providers.single().descriptor.enabled)
    }

    @Test
    fun providersFor_releaseBuildKeepsRealProviderEnabledWhenOnlineIsOn() {
        val registry = DefaultFoodCatalogProviderRegistry(
            entries = listOf(
                ProviderEntry(
                    metadata = descriptor(
                        key = "open_food_facts",
                        enabled = true,
                    ),
                    provider = FakeFoodCatalogProvider(name = "off"),
                    debugDiagnosticsOnly = false,
                ),
            ),
            isDebugBuild = false,
            debugDiagnosticsEnabled = false,
        )

        val providers = registry.providersFor(
            requestType = ProviderRequestType.TEXT_SEARCH,
            onlineLookupEnabled = true,
        )

        assertEquals(1, providers.size)
        assertTrue(providers.single().descriptor.enabled)
    }

    @Test
    fun providersFor_filtersByCapability() {
        val registry = DefaultFoodCatalogProviderRegistry(
            entries = listOf(
                ProviderEntry(
                    metadata = descriptor(
                        key = "text_only",
                        enabled = true,
                    ).copy(
                        supportsTextSearch = true,
                        supportsBarcode = false,
                    ),
                    provider = FakeFoodCatalogProvider(name = "text"),
                ),
                ProviderEntry(
                    metadata = descriptor(
                        key = "barcode_only",
                        enabled = true,
                    ).copy(
                        supportsTextSearch = false,
                        supportsBarcode = true,
                    ),
                    provider = FakeFoodCatalogProvider(name = "barcode"),
                ),
            ),
        )

        val textProviders = registry.providersFor(
            requestType = ProviderRequestType.TEXT_SEARCH,
            onlineLookupEnabled = true,
        )
        val barcodeProviders = registry.providersFor(
            requestType = ProviderRequestType.BARCODE_LOOKUP,
            onlineLookupEnabled = true,
        )

        assertEquals(1, textProviders.size)
        assertEquals("text_only", textProviders.single().descriptor.key)
        assertEquals(1, barcodeProviders.size)
        assertEquals("barcode_only", barcodeProviders.single().descriptor.key)
    }
}

private class FakeFoodCatalogProvider(
    private val name: String,
) : FoodCatalogProvider {
    override suspend fun search(
        query: String,
        token: UserInitiatedNetworkToken,
    ): List<RemoteFoodCandidate> {
        return listOf(
            RemoteFoodCandidate(
                source = RemoteFoodSource.OPEN_FOOD_FACTS,
                sourceId = name,
                barcode = null,
                name = name,
                brand = null,
                caloriesKcalPer100g = null,
                proteinGPer100g = null,
                carbsGPer100g = null,
                fatGPer100g = null,
                servingSize = null,
            ),
        )
    }

    override suspend fun lookupBarcode(
        barcode: String,
        token: UserInitiatedNetworkToken,
    ): RemoteFoodCandidate? {
        return null
    }
}

private fun descriptor(
    key: String,
    priority: Int = 10,
    enabled: Boolean,
    reason: String = "Configured",
): FoodCatalogProviderDescriptor {
    return FoodCatalogProviderDescriptor(
        key = key,
        displayName = key,
        priority = priority,
        supportsBarcode = true,
        supportsTextSearch = true,
        termsOfUseLink = null,
        enabled = enabled,
        statusReason = reason,
    )
}
