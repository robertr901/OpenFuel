package com.openfuel.app.ui.navigation

object Routes {
    const val HOME = "home"
    const val ADD_FOOD = "add-food"
    const val FOOD_DETAIL = "food-detail"
    const val SETTINGS = "settings"

    fun foodDetailRoute(foodId: String): String = "$FOOD_DETAIL/$foodId"
}
