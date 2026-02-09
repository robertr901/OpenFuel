package com.openfuel.app.ui.design

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class OpenFuelDesignTokensTest {
    @Test
    fun spacingScale_remainsStable() {
        assertEquals(4.dp, OFSpacing.x4)
        assertEquals(8.dp, OFSpacing.x8)
        assertEquals(12.dp, OFSpacing.x12)
        assertEquals(16.dp, OFSpacing.x16)
        assertEquals(24.dp, OFSpacing.x24)
        assertEquals(32.dp, OFSpacing.x32)
    }

    @Test
    fun radiusScale_remainsStable() {
        assertEquals(20.dp, OFRadius.card)
        assertEquals(16.dp, OFRadius.control)
        assertEquals(999.dp, OFRadius.pill)
    }
}
