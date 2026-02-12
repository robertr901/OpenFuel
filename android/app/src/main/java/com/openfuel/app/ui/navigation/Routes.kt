package com.openfuel.app.ui.navigation

object Routes {
    const val TODAY = "today"
    const val HISTORY = "history"
    const val FOODS = "foods"
    const val INSIGHTS = "insights"
    const val SETTINGS = "settings"

    const val SELECTED_DATE_ARG = "selectedDate"
    const val TODAY_ROUTE = "$TODAY?$SELECTED_DATE_ARG={$SELECTED_DATE_ARG}"

    const val ADD_FOOD = "add-food"
    const val SCAN_BARCODE = "scan-barcode"
    const val FOOD_DETAIL = "food-detail"
    const val WEEKLY_REVIEW = "weekly-review"

    val topLevelRoutes: Set<String> = setOf(TODAY, HISTORY, FOODS, INSIGHTS, SETTINGS)

    fun todayRoute(selectedDate: String? = null): String {
        return if (selectedDate.isNullOrBlank()) TODAY else "$TODAY?$SELECTED_DATE_ARG=$selectedDate"
    }

    fun foodDetailRoute(foodId: String): String = "$FOOD_DETAIL/$foodId"
}
