package com.openfuel.app.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DecimalInputParserTest {
    @Test
    fun parseDecimalInput_acceptsDotAndCommaDecimals() {
        assertEquals(1.5, parseDecimalInput("1.5") ?: 0.0, 0.0001)
        assertEquals(1.5, parseDecimalInput("1,5") ?: 0.0, 0.0001)
    }

    @Test
    fun parseDecimalInput_rejectsInvalidFormats() {
        assertNull(parseDecimalInput("1..5"))
        assertNull(parseDecimalInput("1,2,3"))
        assertNull(parseDecimalInput("abc"))
    }
}
