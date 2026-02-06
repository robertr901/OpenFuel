package com.openfuel.app.domain

import com.openfuel.app.domain.util.DayWindowCalculator
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DayWindowCalculatorTest {
    @Test
    fun windowFor_hasExpected24HourWindowInUtc() {
        val window = DayWindowCalculator.windowFor(
            date = LocalDate.parse("2024-01-15"),
            zoneId = ZoneId.of("UTC"),
        )

        assertEquals("2024-01-15T00:00:00Z", window.startInclusive.toString())
        assertEquals("2024-01-16T00:00:00Z", window.endExclusive.toString())
        assertEquals(Duration.ofHours(24), Duration.between(window.startInclusive, window.endExclusive))
    }

    @Test
    fun windowFor_respectsZoneAndIsForwardMoving() {
        val zoneId = ZoneId.of("America/Los_Angeles")
        val window = DayWindowCalculator.windowFor(
            date = LocalDate.parse("2024-06-10"),
            zoneId = zoneId,
        )

        assertTrue(window.endExclusive.isAfter(window.startInclusive))
        assertEquals(
            LocalDate.parse("2024-06-10"),
            window.startInclusive.atZone(zoneId).toLocalDate(),
        )
        assertEquals(
            LocalDate.parse("2024-06-11"),
            window.endExclusive.atZone(zoneId).toLocalDate(),
        )
    }
}
