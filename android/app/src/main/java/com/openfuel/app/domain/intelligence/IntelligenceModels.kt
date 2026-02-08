package com.openfuel.app.domain.intelligence

enum class Confidence {
    LOW,
    MEDIUM,
    HIGH,
}

enum class QuantityUnit {
    GRAM,
    KILOGRAM,
    MILLILITRE,
    LITRE,
    CUP,
    TBSP,
    TSP,
    PIECE,
    SERVING,
}

data class FoodTextItem(
    val rawName: String,
    val normalisedName: String,
    val quantity: Double?,
    val unit: QuantityUnit?,
    val notes: String?,
)

data class FoodTextIntent(
    val items: List<FoodTextItem>,
    val confidence: Confidence,
    val warnings: List<String>,
)
