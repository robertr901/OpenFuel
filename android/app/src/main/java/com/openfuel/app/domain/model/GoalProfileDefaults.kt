package com.openfuel.app.domain.model

data class GoalProfileDefaultTargets(
    val caloriesKcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
)

enum class GoalProfileEmphasis {
    CALORIES,
    PROTEIN,
    CARBS,
    BALANCED,
}

object GoalProfileDefaults {
    const val NON_CLINICAL_DISCLAIMER: String =
        "This profile is educational and non-clinical. It is not medical advice."

    fun targetsFor(profile: GoalProfile): GoalProfileDefaultTargets {
        return when (profile) {
            GoalProfile.FAT_LOSS -> GoalProfileDefaultTargets(
                caloriesKcal = 1800.0,
                proteinG = 150.0,
                carbsG = 150.0,
                fatG = 60.0,
            )
            GoalProfile.MUSCLE_GAIN -> GoalProfileDefaultTargets(
                caloriesKcal = 2700.0,
                proteinG = 180.0,
                carbsG = 300.0,
                fatG = 90.0,
            )
            GoalProfile.MAINTENANCE -> GoalProfileDefaultTargets(
                caloriesKcal = 2200.0,
                proteinG = 140.0,
                carbsG = 250.0,
                fatG = 73.0,
            )
            GoalProfile.BLOOD_SUGAR_AWARENESS -> GoalProfileDefaultTargets(
                caloriesKcal = 2000.0,
                proteinG = 140.0,
                carbsG = 170.0,
                fatG = 90.0,
            )
        }
    }

    fun emphasisFor(profile: GoalProfile): GoalProfileEmphasis {
        return when (profile) {
            GoalProfile.FAT_LOSS -> GoalProfileEmphasis.CALORIES
            GoalProfile.MUSCLE_GAIN -> GoalProfileEmphasis.PROTEIN
            GoalProfile.MAINTENANCE -> GoalProfileEmphasis.BALANCED
            GoalProfile.BLOOD_SUGAR_AWARENESS -> GoalProfileEmphasis.CARBS
        }
    }
}

