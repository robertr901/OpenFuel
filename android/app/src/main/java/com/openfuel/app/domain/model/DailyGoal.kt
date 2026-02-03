package com.openfuel.app.domain.model

import java.time.LocalDate

data class DailyGoal(
    val date: LocalDate,
    val caloriesKcalTarget: Double,
    val proteinGTarget: Double,
    val carbsGTarget: Double,
    val fatGTarget: Double,
)
