package com.openfuel.app.domain.util

import com.openfuel.app.domain.model.MacroTotals
import com.openfuel.app.domain.model.MealEntryWithFood
import com.openfuel.app.domain.model.WeeklyReviewSummary
import java.time.LocalDate
import java.time.ZoneId

object WeeklyReviewCalculator {
    fun buildLast7DaySummary(
        entries: List<MealEntryWithFood>,
        today: LocalDate,
        zoneId: ZoneId,
    ): WeeklyReviewSummary {
        val startDate = today.minusDays(6)
        val totalsByDate = entries
            .groupBy { it.entry.timestamp.atZone(zoneId).toLocalDate() }
            .mapValues { (_, dayEntries) -> MealTotalsCalculator.totalsFor(dayEntries).totals }

        var cursor = startDate
        val totalsInRange = mutableListOf<MacroTotals>()
        while (!cursor.isAfter(today)) {
            totalsByDate[cursor]?.let { totalsInRange += it }
            cursor = cursor.plusDays(1)
        }

        val total = totalsInRange.fold(MacroTotals.Zero) { acc, current ->
            MacroTotals(
                caloriesKcal = acc.caloriesKcal + current.caloriesKcal,
                proteinG = acc.proteinG + current.proteinG,
                carbsG = acc.carbsG + current.carbsG,
                fatG = acc.fatG + current.fatG,
            )
        }

        val loggedDays = totalsInRange.size
        val average = if (loggedDays == 0) {
            MacroTotals.Zero
        } else {
            MacroTotals(
                caloriesKcal = total.caloriesKcal / loggedDays.toDouble(),
                proteinG = total.proteinG / loggedDays.toDouble(),
                carbsG = total.carbsG / loggedDays.toDouble(),
                fatG = total.fatG / loggedDays.toDouble(),
            )
        }

        val unknownEntryCount = entries.count { entry ->
            entry.food.caloriesKcal <= 0.0 &&
                entry.food.proteinG <= 0.0 &&
                entry.food.carbsG <= 0.0 &&
                entry.food.fatG <= 0.0
        }

        return WeeklyReviewSummary(
            startDate = startDate,
            endDate = today,
            loggedDays = loggedDays,
            missingDays = (7 - loggedDays).coerceAtLeast(0),
            unknownEntryCount = unknownEntryCount,
            total = total,
            average = average,
        )
    }
}
