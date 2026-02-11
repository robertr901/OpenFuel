package com.openfuel.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GoalProfileDefaultsTest {
    @Test
    fun targetsFor_allProfiles_areDeterministic() {
        assertEquals(
            GoalProfileDefaultTargets(1800.0, 150.0, 150.0, 60.0),
            GoalProfileDefaults.targetsFor(GoalProfile.FAT_LOSS),
        )
        assertEquals(
            GoalProfileDefaultTargets(2700.0, 180.0, 300.0, 90.0),
            GoalProfileDefaults.targetsFor(GoalProfile.MUSCLE_GAIN),
        )
        assertEquals(
            GoalProfileDefaultTargets(2200.0, 140.0, 250.0, 73.0),
            GoalProfileDefaults.targetsFor(GoalProfile.MAINTENANCE),
        )
        assertEquals(
            GoalProfileDefaultTargets(2000.0, 140.0, 170.0, 90.0),
            GoalProfileDefaults.targetsFor(GoalProfile.BLOOD_SUGAR_AWARENESS),
        )
    }

    @Test
    fun emphasisFor_allProfiles_isStable() {
        assertEquals(GoalProfileEmphasis.CALORIES, GoalProfileDefaults.emphasisFor(GoalProfile.FAT_LOSS))
        assertEquals(GoalProfileEmphasis.PROTEIN, GoalProfileDefaults.emphasisFor(GoalProfile.MUSCLE_GAIN))
        assertEquals(GoalProfileEmphasis.BALANCED, GoalProfileDefaults.emphasisFor(GoalProfile.MAINTENANCE))
        assertEquals(GoalProfileEmphasis.CARBS, GoalProfileDefaults.emphasisFor(GoalProfile.BLOOD_SUGAR_AWARENESS))
    }
}

