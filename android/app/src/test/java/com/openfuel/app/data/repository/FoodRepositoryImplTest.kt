package com.openfuel.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class FoodRepositoryImplTest {
    @Test
    fun escapeLikeQuery_escapesSqlWildcardsAndBackslash() {
        val escaped = escapeLikeQuery("100%_\\test")

        assertEquals("100\\%\\_\\\\test", escaped)
    }
}
