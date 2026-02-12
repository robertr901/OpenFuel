package com.openfuel.app.domain.util

import com.openfuel.app.domain.model.DailyGoal
import com.openfuel.app.domain.model.GoalProfileEmphasis
import com.openfuel.app.domain.model.MacroTotals
import com.openfuel.app.domain.model.WeeklyReviewSuggestionReason
import com.openfuel.app.domain.model.WeeklyReviewSummary
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WeeklyReviewSuggestionEngineTest {
    @Test
    fun suggest_returnsNull_whenLoggedDaysAreInsufficient() {
        val suggestion = WeeklyReviewSuggestionEngine.suggest(
            summary = summary(loggedDays = 2, missingDays = 5),
            goal = null,
            emphasis = GoalProfileEmphasis.BALANCED,
        )

        assertNull(suggestion)
    }

    @Test
    fun suggest_prefersConsistency_whenManyDaysMissing() {
        val suggestion = WeeklyReviewSuggestionEngine.suggest(
            summary = summary(loggedDays = 3, missingDays = 4),
            goal = null,
            emphasis = GoalProfileEmphasis.BALANCED,
        )

        assertNotNull(suggestion)
        assertEquals(WeeklyReviewSuggestionReason.IMPROVE_LOGGING_CONSISTENCY, suggestion?.reason)
    }

    @Test
    fun suggest_usesProteinRule_forProteinEmphasis() {
        val suggestion = WeeklyReviewSuggestionEngine.suggest(
            summary = summary(
                loggedDays = 5,
                missingDays = 2,
                average = MacroTotals(caloriesKcal = 2100.0, proteinG = 90.0, carbsG = 220.0, fatG = 70.0),
            ),
            goal = goal(protein = 140.0),
            emphasis = GoalProfileEmphasis.PROTEIN,
        )

        assertNotNull(suggestion)
        assertEquals(WeeklyReviewSuggestionReason.IMPROVE_PROTEIN_COVERAGE, suggestion?.reason)
    }

    @Test
    fun suggest_usesCarbRule_forCarbEmphasis() {
        val suggestion = WeeklyReviewSuggestionEngine.suggest(
            summary = summary(
                loggedDays = 5,
                missingDays = 2,
                average = MacroTotals(caloriesKcal = 2200.0, proteinG = 130.0, carbsG = 250.0, fatG = 70.0),
            ),
            goal = goal(carbs = 200.0),
            emphasis = GoalProfileEmphasis.CARBS,
        )

        assertNotNull(suggestion)
        assertEquals(WeeklyReviewSuggestionReason.SUPPORT_CARB_STABILITY, suggestion?.reason)
    }

    @Test
    fun suggest_returnsMaintainPattern_whenNoOtherRuleMatches() {
        val suggestion = WeeklyReviewSuggestionEngine.suggest(
            summary = summary(
                loggedDays = 6,
                missingDays = 1,
                average = MacroTotals(caloriesKcal = 2050.0, proteinG = 140.0, carbsG = 230.0, fatG = 75.0),
            ),
            goal = goal(
                calories = 2100.0,
                protein = 135.0,
                carbs = 235.0,
                fat = 75.0,
            ),
            emphasis = GoalProfileEmphasis.BALANCED,
        )

        assertNotNull(suggestion)
        assertEquals(WeeklyReviewSuggestionReason.MAINTAIN_PATTERN, suggestion?.reason)
    }

    private fun summary(
        loggedDays: Int,
        missingDays: Int,
        average: MacroTotals = MacroTotals(
            caloriesKcal = 1800.0,
            proteinG = 120.0,
            carbsG = 190.0,
            fatG = 60.0,
        ),
    ): WeeklyReviewSummary {
        return WeeklyReviewSummary(
            startDate = LocalDate.parse("2026-02-06"),
            endDate = LocalDate.parse("2026-02-12"),
            loggedDays = loggedDays,
            missingDays = missingDays,
            unknownEntryCount = 0,
            total = MacroTotals(
                caloriesKcal = average.caloriesKcal * loggedDays,
                proteinG = average.proteinG * loggedDays,
                carbsG = average.carbsG * loggedDays,
                fatG = average.fatG * loggedDays,
            ),
            average = average,
        )
    }

    private fun goal(
        calories: Double = 2000.0,
        protein: Double = 120.0,
        carbs: Double = 220.0,
        fat: Double = 70.0,
    ): DailyGoal {
        return DailyGoal(
            date = LocalDate.parse("2026-02-12"),
            caloriesKcalTarget = calories,
            proteinGTarget = protein,
            carbsGTarget = carbs,
            fatGTarget = fat,
        )
    }
}
