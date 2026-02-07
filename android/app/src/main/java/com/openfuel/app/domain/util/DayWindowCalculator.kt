package com.openfuel.app.domain.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DayWindow(
    val startInclusive: Instant,
    val endExclusive: Instant,
)

object DayWindowCalculator {
    fun windowFor(date: LocalDate, zoneId: ZoneId): DayWindow {
        val start = date.atStartOfDay(zoneId).toInstant()
        val end = date.plusDays(1).atStartOfDay(zoneId).toInstant()
        return DayWindow(startInclusive = start, endExclusive = end)
    }
}
