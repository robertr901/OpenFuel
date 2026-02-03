package com.openfuel.app.domain.model

enum class FoodUnit {
    SERVING,
    GRAM,
}

fun FoodUnit.displayName(): String = when (this) {
    FoodUnit.SERVING -> "Serving"
    FoodUnit.GRAM -> "Gram"
}

fun FoodUnit.shortLabel(): String = when (this) {
    FoodUnit.SERVING -> "serv"
    FoodUnit.GRAM -> "g"
}
