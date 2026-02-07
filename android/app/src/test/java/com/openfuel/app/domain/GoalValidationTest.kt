package com.openfuel.app.domain

import com.openfuel.app.domain.util.GoalValidation
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalValidationTest {
    @Test
    fun isValidCalories_respectsBounds() {
        assertTrue(GoalValidation.isValidCalories(0.0))
        assertTrue(GoalValidation.isValidCalories(5000.0))
        assertTrue(GoalValidation.isValidCalories(10000.0))
        assertFalse(GoalValidation.isValidCalories(-1.0))
        assertFalse(GoalValidation.isValidCalories(10000.1))
    }

    @Test
    fun isValidMacro_respectsBounds() {
        assertTrue(GoalValidation.isValidMacro(0.0))
        assertTrue(GoalValidation.isValidMacro(350.5))
        assertTrue(GoalValidation.isValidMacro(1000.0))
        assertFalse(GoalValidation.isValidMacro(-0.1))
        assertFalse(GoalValidation.isValidMacro(1000.1))
    }
}
