package com.openfuel.app.domain.model

enum class GoalProfile {
    FAT_LOSS,
    MUSCLE_GAIN,
    MAINTENANCE,
    BLOOD_SUGAR_AWARENESS,
}

enum class DietaryOverlay {
    LOW_FODMAP,
    LOW_SODIUM,
}

data class GoalProfileSelection(
    val profile: GoalProfile?,
    val overlays: Set<DietaryOverlay>,
)

