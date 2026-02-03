package com.openfuel.app.domain.util

import com.openfuel.app.domain.model.MacroTotals
import com.openfuel.app.domain.model.MealEntryWithFood
import com.openfuel.app.domain.model.MealTotals
import com.openfuel.app.domain.model.MealType

object MealTotalsCalculator {
    fun totalsFor(entries: List<MealEntryWithFood>): MealTotals {
        val totals = entries.fold(MacroTotals.Zero) { acc, entryWithFood ->
            val macros = UnitConversion.scaleMacros(
                foodItem = entryWithFood.food,
                quantity = entryWithFood.entry.quantity,
                unit = entryWithFood.entry.unit,
            )
            MacroTotals(
                caloriesKcal = acc.caloriesKcal + macros.caloriesKcal,
                proteinG = acc.proteinG + macros.proteinG,
                carbsG = acc.carbsG + macros.carbsG,
                fatG = acc.fatG + macros.fatG,
            )
        }
        return MealTotals(totals)
    }

    fun totalsByMealType(entries: List<MealEntryWithFood>): Map<MealType, MealTotals> {
        return entries.groupBy { it.entry.mealType }
            .mapValues { (_, groupedEntries) -> totalsFor(groupedEntries) }
    }
}
