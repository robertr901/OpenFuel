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

interface FoodCatalogProviderRegistry {
    fun primaryTextSearchProvider(): FoodCatalogProvider

    fun providerDiagnostics(): List<FoodCatalogProviderDescriptor>
}
