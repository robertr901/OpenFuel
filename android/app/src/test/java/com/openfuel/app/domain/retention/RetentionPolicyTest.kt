package com.openfuel.app.domain.retention

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RetentionPolicyTest {
    private val defaultSettings = FastLogReminderSettings(
        enabled = true,
        reminderWindowStartHour = 7,
        reminderWindowEndHour = 21,
        quietHoursEnabled = true,
        quietHoursStartHour = 21,
        quietHoursEndHour = 7,
        maxImpressionsPerDay = 1,
        maxImpressionsPerSession = 1,
        dismissalsBeforeCooldown = 3,
        cooldownDays = 3,
    )

    @Test
    fun evaluateFastLogReminder_showsWhenAllConditionsPass() {
        val evaluation = RetentionPolicy.evaluateFastLogReminder(
            settings = defaultSettings,
            context = contextAtHour(hour = 12),
        )

        assertTrue(evaluation.shouldShow)
        assertEquals(FastLogReminderDecision.SHOW, evaluation.decision)
    }

    @Test
    fun evaluateFastLogReminder_hidesDuringQuietHours() {
        val evaluation = RetentionPolicy.evaluateFastLogReminder(
            settings = defaultSettings.copy(
                reminderWindowStartHour = 0,
                reminderWindowEndHour = 0,
            ),
            context = contextAtHour(hour = 22),
        )

        assertFalse(evaluation.shouldShow)
        assertEquals(FastLogReminderDecision.HIDE_QUIET_HOURS, evaluation.decision)
    }

    @Test
    fun evaluateFastLogReminder_hidesWhenDailyCapReached() {
        val evaluation = RetentionPolicy.evaluateFastLogReminder(
            settings = defaultSettings,
            context = contextAtHour(hour = 12, impressionsToday = 1),
        )

        assertFalse(evaluation.shouldShow)
        assertEquals(FastLogReminderDecision.HIDE_DAILY_CAP, evaluation.decision)
    }

    @Test
    fun evaluateFastLogReminder_hidesWhenSessionCapReached() {
        val evaluation = RetentionPolicy.evaluateFastLogReminder(
            settings = defaultSettings.copy(maxImpressionsPerDay = 2),
            context = contextAtHour(hour = 12, impressionsThisSession = 1),
        )

        assertFalse(evaluation.shouldShow)
        assertEquals(FastLogReminderDecision.HIDE_SESSION_CAP, evaluation.decision)
    }

    @Test
    fun evaluateFastLogReminder_hidesForCooldownWindow() {
        val evaluation = RetentionPolicy.evaluateFastLogReminder(
            settings = defaultSettings,
            context = contextAtHour(
                hour = 12,
                consecutiveDismissals = 3,
                lastDismissedDate = LocalDate.of(2026, 2, 10),
                now = LocalDateTime.of(2026, 2, 11, 12, 0),
            ),
        )

        assertFalse(evaluation.shouldShow)
        assertEquals(FastLogReminderDecision.HIDE_COOLDOWN, evaluation.decision)
    }

    @Test
    fun evaluateFastLogReminder_allowsAfterCooldownExpires() {
        val evaluation = RetentionPolicy.evaluateFastLogReminder(
            settings = defaultSettings,
            context = contextAtHour(
                hour = 12,
                consecutiveDismissals = 3,
                lastDismissedDate = LocalDate.of(2026, 2, 7),
                now = LocalDateTime.of(2026, 2, 11, 12, 0),
            ),
        )

        assertTrue(evaluation.shouldShow)
        assertEquals(FastLogReminderDecision.SHOW, evaluation.decision)
    }

    @Test
    fun evaluateFastLogReminder_hidesWhenAlreadyLoggedToday() {
        val evaluation = RetentionPolicy.evaluateFastLogReminder(
            settings = defaultSettings,
            context = contextAtHour(hour = 12, hasLoggedToday = true),
        )

        assertFalse(evaluation.shouldShow)
        assertEquals(FastLogReminderDecision.HIDE_ALREADY_LOGGED, evaluation.decision)
    }

    private fun contextAtHour(
        hour: Int,
        impressionsToday: Int = 0,
        impressionsThisSession: Int = 0,
        hasLoggedToday: Boolean = false,
        consecutiveDismissals: Int = 0,
        lastDismissedDate: LocalDate? = null,
        now: LocalDateTime = LocalDateTime.of(2026, 2, 11, hour, 0),
    ): FastLogReminderContext {
        return FastLogReminderContext(
            now = now,
            hasLoggedToday = hasLoggedToday,
            impressionsToday = impressionsToday,
            impressionsThisSession = impressionsThisSession,
            consecutiveDismissals = consecutiveDismissals,
            lastDismissedDate = lastDismissedDate,
        )
    }
}
