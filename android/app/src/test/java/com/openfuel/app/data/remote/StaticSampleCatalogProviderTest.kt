package com.openfuel.app.data.remote

import com.openfuel.app.domain.model.RemoteFoodSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StaticSampleCatalogProviderTest {
    private val guard = UserInitiatedNetworkGuard()

    @Test
    fun search_returnsDeterministicSamplesForKnownQuery() = runTest {
        val provider = StaticSampleCatalogProvider()

        val results = provider.search(
            query = "oat",
            token = guard.issueToken("search"),
        )

        assertEquals(2, results.size)
        assertEquals(RemoteFoodSource.STATIC_SAMPLE, results[0].source)
        assertEquals("sample-oatmeal-1", results[0].sourceId)
        assertEquals("sample-oat-milk-2", results[1].sourceId)
    }

    @Test
    fun lookupBarcode_returnsExpectedDeterministicFood() = runTest {
        val provider = StaticSampleCatalogProvider()

        val result = provider.lookupBarcode(
            barcode = "0001112223333",
            token = guard.issueToken("lookup"),
        )

        assertTrue(result != null)
        assertEquals(RemoteFoodSource.STATIC_SAMPLE, result?.source)
        assertEquals("Sample Oatmeal", result?.name)
    }
}
