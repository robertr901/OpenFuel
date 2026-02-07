package com.openfuel.app.domain.util

import com.openfuel.app.domain.model.InsightWindow
import com.openfuel.app.domain.model.InsightsSnapshot
import com.openfuel.app.domain.model.MacroTotals
import com.openfuel.app.domain.model.MealEntryWithFood
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

object InsightsCalculator {
    fun buildSnapshot(
        entries: List<MealEntryWithFood>,
        today: LocalDate,
        zoneId: ZoneId,
    ): InsightsSnapshot {
        val totalsByDate = entries
            .groupBy { entry -> entry.entry.timestamp.atZone(zoneId).toLocalDate() }
            .mapValues { (_, dayEntries) -> MealTotalsCalculator.totalsFor(dayEntries).totals }

        val last7 = buildWindow(
            totalsByDate = totalsByDate,
            endDate = today,
            dayCount = 7,
            label = "Last 7 days",
        )
        val last30 = buildWindow(
            totalsByDate = totalsByDate,
            endDate = today,
            dayCount = 30,
            label = "Last 30 days",
        )
        return InsightsSnapshot(
            last7Days = last7,
            last30Days = last30,
            consistencyScore = consistencyScore(last30.loggedDays, 30),
        )
    }

    private fun buildWindow(
        totalsByDate: Map<LocalDate, MacroTotals>,
        endDate: LocalDate,
        dayCount: Int,
        label: String,
    ): InsightWindow {
        val startDate = endDate.minusDays((dayCount - 1).toLong())
        val rangeTotals = mutableListOf<MacroTotals>()
        var cursor = startDate
        while (!cursor.isAfter(endDate)) {
            totalsByDate[cursor]?.let { dayTotals ->
                rangeTotals += dayTotals
            }
            cursor = cursor.plusDays(1)
        }

        val total = rangeTotals.fold(MacroTotals.Zero) { acc, totals ->
            MacroTotals(
                caloriesKcal = acc.caloriesKcal + totals.caloriesKcal,
                proteinG = acc.proteinG + totals.proteinG,
                carbsG = acc.carbsG + totals.carbsG,
                fatG = acc.fatG + totals.fatG,
            )
        }
        val divisor = if (rangeTotals.isEmpty()) 1.0 else rangeTotals.size.toDouble()
        val average = MacroTotals(
            caloriesKcal = total.caloriesKcal / divisor,
            proteinG = total.proteinG / divisor,
            carbsG = total.carbsG / divisor,
            fatG = total.fatG / divisor,
        )

        return InsightWindow(
            label = label,
            startDate = startDate,
            endDate = endDate,
            loggedDays = rangeTotals.size,
            total = total,
            average = average,
        )
    }

    private fun consistencyScore(loggedDays: Int, totalDays: Int): Int {
        if (totalDays <= 0) return 0
        return ((loggedDays.toDouble() / totalDays.toDouble()) * 100.0)
            .roundToInt()
            .coerceIn(0, 100)
    }
}
