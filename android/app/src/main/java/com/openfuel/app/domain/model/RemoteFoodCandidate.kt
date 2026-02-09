package com.openfuel.app.domain.model

data class RemoteFoodCandidate(
    val source: RemoteFoodSource,
    val sourceId: String,
    val providerKey: String? = null,
    val barcode: String?,
    val name: String,
    val brand: String?,
    val caloriesKcalPer100g: Double?,
    val proteinGPer100g: Double?,
    val carbsGPer100g: Double?,
    val fatGPer100g: Double?,
    val servingSize: String?,
)

enum class RemoteFoodSource {
    OPEN_FOOD_FACTS,
    STATIC_SAMPLE,
    USDA_FOODDATA_CENTRAL,
}
