package com.openfuel.app.data.remote

import com.openfuel.app.domain.service.FoodCatalogProvider
import com.openfuel.app.domain.service.FoodCatalogProviderDescriptor
import com.openfuel.app.domain.service.FoodCatalogProviderRegistry

class DefaultFoodCatalogProviderRegistry(
    entries: List<ProviderEntry>,
) : FoodCatalogProviderRegistry {
    private val providerEntries: List<ProviderEntry> = entries.sortedBy { it.metadata.priority }

    override fun primaryTextSearchProvider(): FoodCatalogProvider {
        return providerEntries
            .firstOrNull { entry ->
                entry.metadata.enabled &&
                    entry.metadata.supportsTextSearch &&
                    entry.provider != null
            }
            ?.provider
            ?: error("No enabled text-search provider configured.")
    }

    override fun providerDiagnostics(): List<FoodCatalogProviderDescriptor> {
        return providerEntries.map { it.metadata }
    }
}

data class ProviderEntry(
    val metadata: FoodCatalogProviderDescriptor,
    val provider: FoodCatalogProvider?,
)
