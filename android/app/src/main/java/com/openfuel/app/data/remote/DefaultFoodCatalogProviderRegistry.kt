package com.openfuel.app.data.remote

import com.openfuel.app.domain.service.FoodCatalogExecutionProvider
import com.openfuel.app.domain.service.FoodCatalogProvider
import com.openfuel.app.domain.service.FoodCatalogProviderDescriptor
import com.openfuel.app.domain.service.FoodCatalogProviderRegistry
import com.openfuel.app.domain.service.ProviderRequestType

class DefaultFoodCatalogProviderRegistry(
    entries: List<ProviderEntry>,
    private val isDebugBuild: Boolean = true,
    private val debugDiagnosticsEnabled: Boolean = true,
) : FoodCatalogProviderRegistry {
    private val providerEntries: List<ProviderEntry> = entries.sortedBy { it.metadata.priority }

    override fun providersFor(
        requestType: ProviderRequestType,
        onlineLookupEnabled: Boolean,
    ): List<FoodCatalogExecutionProvider> {
        return providerEntries
            .filter { entry -> entry.supports(requestType) }
            .map { entry ->
                val resolvedDescriptor = resolveDescriptor(
                    entry = entry,
                    onlineLookupEnabled = onlineLookupEnabled,
                )
                FoodCatalogExecutionProvider(
                    descriptor = resolvedDescriptor,
                    provider = entry.provider,
                )
            }
    }

    override fun primaryTextSearchProvider(): FoodCatalogProvider {
        return providersFor(
            requestType = ProviderRequestType.TEXT_SEARCH,
            onlineLookupEnabled = true,
        ).firstOrNull { provider ->
            provider.descriptor.enabled && provider.provider != null
        }?.provider ?: error("No enabled text-search provider configured.")
    }

    override fun providerDiagnostics(onlineLookupEnabled: Boolean): List<FoodCatalogProviderDescriptor> {
        return providerEntries.map { entry ->
            resolveDescriptor(
                entry = entry,
                onlineLookupEnabled = onlineLookupEnabled,
            )
        }
    }

    private fun resolveDescriptor(
        entry: ProviderEntry,
        onlineLookupEnabled: Boolean,
    ): FoodCatalogProviderDescriptor {
        if (entry.debugDiagnosticsOnly && (!isDebugBuild || !debugDiagnosticsEnabled)) {
            return entry.metadata.copy(
                enabled = false,
                statusReason = "Debug diagnostics disabled.",
            )
        }
        if (!entry.metadata.enabled) {
            return entry.metadata
        }
        if (!onlineLookupEnabled) {
            return entry.metadata.copy(
                enabled = false,
                statusReason = "Online lookup disabled in Settings.",
            )
        }
        if (entry.provider == null) {
            return entry.metadata.copy(
                enabled = false,
                statusReason = "Provider is not configured.",
            )
        }
        return entry.metadata.copy(
            enabled = true,
            statusReason = "Active",
        )
    }

    private fun ProviderEntry.supports(requestType: ProviderRequestType): Boolean {
        return when (requestType) {
            ProviderRequestType.TEXT_SEARCH -> metadata.supportsTextSearch
            ProviderRequestType.BARCODE_LOOKUP -> metadata.supportsBarcode
        }
    }
}

data class ProviderEntry(
    val metadata: FoodCatalogProviderDescriptor,
    val provider: FoodCatalogProvider?,
    val debugDiagnosticsOnly: Boolean = false,
)
