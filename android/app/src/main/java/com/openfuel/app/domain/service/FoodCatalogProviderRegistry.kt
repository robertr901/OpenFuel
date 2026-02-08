package com.openfuel.app.domain.service

data class FoodCatalogProviderDescriptor(
    val key: String,
    val displayName: String,
    val priority: Int,
    val supportsBarcode: Boolean,
    val supportsTextSearch: Boolean,
    val termsOfUseLink: String?,
    val enabled: Boolean,
    val statusReason: String,
)

data class FoodCatalogExecutionProvider(
    val descriptor: FoodCatalogProviderDescriptor,
    val provider: FoodCatalogProvider?,
) {
    fun supports(capability: ProviderCapability): Boolean {
        return when (capability) {
            ProviderCapability.TEXT_SEARCH -> descriptor.supportsTextSearch
            ProviderCapability.BARCODE_LOOKUP -> descriptor.supportsBarcode
        }
    }
}

interface FoodCatalogProviderRegistry {
    fun providersFor(
        requestType: ProviderRequestType,
        onlineLookupEnabled: Boolean,
    ): List<FoodCatalogExecutionProvider>

    fun primaryTextSearchProvider(): FoodCatalogProvider

    fun providerDiagnostics(onlineLookupEnabled: Boolean = true): List<FoodCatalogProviderDescriptor>
}
