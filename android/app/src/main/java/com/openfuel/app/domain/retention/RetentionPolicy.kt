package com.openfuel.app.domain.retention

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

data class FastLogReminderSettings(
    val enabled: Boolean,
    val reminderWindowStartHour: Int,
    val reminderWindowEndHour: Int,
    val quietHoursEnabled: Boolean,
    val quietHoursStartHour: Int,
    val quietHoursEndHour: Int,
    val maxImpressionsPerDay: Int = 1,
    val maxImpressionsPerSession: Int = 1,
    val dismissalsBeforeCooldown: Int = 3,
    val cooldownDays: Int = 3,
)

data class FastLogReminderContext(
    val now: LocalDateTime,
    val hasLoggedToday: Boolean,
    val impressionsToday: Int,
    val impressionsThisSession: Int,
    val consecutiveDismissals: Int,
    val lastDismissedDate: LocalDate?,
)

enum class FastLogReminderDecision {
    SHOW,
    HIDE_DISABLED,
    HIDE_ALREADY_LOGGED,
    HIDE_OUTSIDE_REMINDER_WINDOW,
    HIDE_QUIET_HOURS,
    HIDE_DAILY_CAP,
    HIDE_SESSION_CAP,
    HIDE_COOLDOWN,
}

data class FastLogReminderEvaluation(
    val decision: FastLogReminderDecision,
) {
    val shouldShow: Boolean
        get() = decision == FastLogReminderDecision.SHOW
}

object RetentionPolicy {
    fun evaluateFastLogReminder(
        settings: FastLogReminderSettings,
        context: FastLogReminderContext,
    ): FastLogReminderEvaluation {
        if (!settings.enabled) {
            return FastLogReminderEvaluation(FastLogReminderDecision.HIDE_DISABLED)
        }

        if (context.hasLoggedToday) {
            return FastLogReminderEvaluation(FastLogReminderDecision.HIDE_ALREADY_LOGGED)
        }

        val currentHour = context.now.toLocalTime().hour
        if (!isWithinWindow(currentHour, settings.reminderWindowStartHour, settings.reminderWindowEndHour)) {
            return FastLogReminderEvaluation(FastLogReminderDecision.HIDE_OUTSIDE_REMINDER_WINDOW)
        }

        if (
            settings.quietHoursEnabled &&
            isWithinWindow(currentHour, settings.quietHoursStartHour, settings.quietHoursEndHour)
        ) {
            return FastLogReminderEvaluation(FastLogReminderDecision.HIDE_QUIET_HOURS)
        }

        if (context.impressionsToday >= settings.maxImpressionsPerDay) {
            return FastLogReminderEvaluation(FastLogReminderDecision.HIDE_DAILY_CAP)
        }

        if (context.impressionsThisSession >= settings.maxImpressionsPerSession) {
            return FastLogReminderEvaluation(FastLogReminderDecision.HIDE_SESSION_CAP)
        }

        if (
            context.consecutiveDismissals >= settings.dismissalsBeforeCooldown &&
            isWithinCooldown(context, settings.cooldownDays)
        ) {
            return FastLogReminderEvaluation(FastLogReminderDecision.HIDE_COOLDOWN)
        }

        return FastLogReminderEvaluation(FastLogReminderDecision.SHOW)
    }

    private fun isWithinCooldown(
        context: FastLogReminderContext,
        cooldownDays: Int,
    ): Boolean {
        if (cooldownDays <= 0) return false
        val lastDismissedDate = context.lastDismissedDate ?: return false
        val daysSinceDismiss = ChronoUnit.DAYS.between(lastDismissedDate, context.now.toLocalDate())
        return daysSinceDismiss < cooldownDays
    }

    private fun isWithinWindow(
        currentHour: Int,
        startHour: Int,
        endHour: Int,
    ): Boolean {
        val normalizedCurrent = LocalTime.of(currentHour.coerceIn(0, 23), 0)
        val normalizedStart = LocalTime.of(startHour.coerceIn(0, 23), 0)
        val normalizedEnd = LocalTime.of(endHour.coerceIn(0, 23), 0)

        if (normalizedStart == normalizedEnd) {
            return true
        }

        return if (normalizedStart.isBefore(normalizedEnd)) {
            !normalizedCurrent.isBefore(normalizedStart) && normalizedCurrent.isBefore(normalizedEnd)
        } else {
            !normalizedCurrent.isBefore(normalizedStart) || normalizedCurrent.isBefore(normalizedEnd)
        }
    }
}
