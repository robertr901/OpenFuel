package com.openfuel.app.domain.util

import com.openfuel.app.domain.model.DailyGoal
import com.openfuel.app.domain.model.GoalProfileEmphasis
import com.openfuel.app.domain.model.WeeklyReviewSuggestion
import com.openfuel.app.domain.model.WeeklyReviewSuggestionReason
import com.openfuel.app.domain.model.WeeklyReviewSummary

object WeeklyReviewSuggestionEngine {
    fun suggest(
        summary: WeeklyReviewSummary,
        goal: DailyGoal?,
        emphasis: GoalProfileEmphasis?,
    ): WeeklyReviewSuggestion? {
        if (summary.loggedDays < 3) return null

        if (summary.missingDays >= 3) {
            return WeeklyReviewSuggestion(
                title = "Build steadier coverage",
                action = "Log one extra meal on two additional days this week.",
                why = "This review is based on ${summary.loggedDays} of the last 7 days.",
                reason = WeeklyReviewSuggestionReason.IMPROVE_LOGGING_CONSISTENCY,
            )
        }

        if (emphasis == GoalProfileEmphasis.PROTEIN) {
            val proteinGoal = goal?.proteinGTarget ?: 0.0
            if (proteinGoal > 0.0 && summary.average.proteinG < proteinGoal * 0.85) {
                return WeeklyReviewSuggestion(
                    title = "Support protein target",
                    action = "Add one protein-focused item to one meal tomorrow.",
                    why = "Average protein is below your current target.",
                    reason = WeeklyReviewSuggestionReason.IMPROVE_PROTEIN_COVERAGE,
                )
            }
        }

        if (emphasis == GoalProfileEmphasis.CARBS) {
            val carbsGoal = goal?.carbsGTarget ?: 0.0
            if (carbsGoal > 0.0 && summary.average.carbsG > carbsGoal * 1.1) {
                return WeeklyReviewSuggestion(
                    title = "Smooth carb intake",
                    action = "Replace one high-carb extra with a balanced option once this week.",
                    why = "Average carbs are above your current target.",
                    reason = WeeklyReviewSuggestionReason.SUPPORT_CARB_STABILITY,
                )
            }
        }

        val caloriesGoal = goal?.caloriesKcalTarget ?: 0.0
        if (caloriesGoal > 0.0) {
            if (summary.average.caloriesKcal > caloriesGoal * 1.1) {
                return WeeklyReviewSuggestion(
                    title = "Nudge calories toward target",
                    action = "Trim one high-calorie extra from one meal this week.",
                    why = "Average calories are above your current target.",
                    reason = WeeklyReviewSuggestionReason.ALIGN_CALORIE_TARGET,
                )
            }

            if (summary.average.caloriesKcal < caloriesGoal * 0.8) {
                return WeeklyReviewSuggestion(
                    title = "Support energy consistency",
                    action = "Add one balanced snack on a lower-intake day this week.",
                    why = "Average calories are below your current target.",
                    reason = WeeklyReviewSuggestionReason.ALIGN_CALORIE_TARGET,
                )
            }
        }

        return WeeklyReviewSuggestion(
            title = "Keep current pattern",
            action = "Repeat this week's logging pattern and review again next week.",
            why = "Current averages are broadly aligned with your recent targets.",
            reason = WeeklyReviewSuggestionReason.MAINTAIN_PATTERN,
        )
    }
}
