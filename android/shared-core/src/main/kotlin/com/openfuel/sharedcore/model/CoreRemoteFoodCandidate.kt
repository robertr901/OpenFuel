package com.openfuel.sharedcore.model

data class CoreRemoteFoodCandidate(
    val source: String,
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
