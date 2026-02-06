package com.openfuel.app.ui.navigation

object Routes {
    const val TODAY = "today"
    const val FOODS = "foods"
    const val SETTINGS = "settings"

    const val ADD_FOOD = "add-food"
    const val SCAN_BARCODE = "scan-barcode"
    const val FOOD_DETAIL = "food-detail"

    val topLevelRoutes: Set<String> = setOf(TODAY, FOODS, SETTINGS)

    fun foodDetailRoute(foodId: String): String = "$FOOD_DETAIL/$foodId"
}
