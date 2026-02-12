package com.openfuel.app.domain.model

import java.time.LocalDate

data class WeeklyReviewSummary(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val loggedDays: Int,
    val missingDays: Int,
    val unknownEntryCount: Int,
    val total: MacroTotals,
    val average: MacroTotals,
)

data class WeeklyReviewSuggestion(
    val title: String,
    val action: String,
    val why: String,
    val reason: WeeklyReviewSuggestionReason,
    val tone: WeeklyReviewUiTone = WeeklyReviewUiTone.CALM,
)

enum class WeeklyReviewSuggestionReason {
    IMPROVE_LOGGING_CONSISTENCY,
    IMPROVE_PROTEIN_COVERAGE,
    SUPPORT_CARB_STABILITY,
    ALIGN_CALORIE_TARGET,
    MAINTAIN_PATTERN,
}

enum class WeeklyReviewUiTone {
    CALM,
}
