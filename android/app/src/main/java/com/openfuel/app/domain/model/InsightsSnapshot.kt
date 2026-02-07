package com.openfuel.app.domain.model

import java.time.LocalDate

data class InsightsSnapshot(
    val last7Days: InsightWindow,
    val last30Days: InsightWindow,
    val consistencyScore: Int,
) {
    companion object {
        fun empty(today: LocalDate): InsightsSnapshot {
            return InsightsSnapshot(
                last7Days = InsightWindow.empty(today),
                last30Days = InsightWindow.empty(today),
                consistencyScore = 0,
            )
        }
    }
}

data class InsightWindow(
    val label: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val loggedDays: Int,
    val total: MacroTotals,
    val average: MacroTotals,
) {
    companion object {
        fun empty(today: LocalDate): InsightWindow {
            return InsightWindow(
                label = "",
                startDate = today,
                endDate = today,
                loggedDays = 0,
                total = MacroTotals.Zero,
                average = MacroTotals.Zero,
            )
        }
    }
}
