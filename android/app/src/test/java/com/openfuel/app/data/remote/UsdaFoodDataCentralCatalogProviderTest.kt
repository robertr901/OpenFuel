package com.openfuel.app.data.remote

import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.domain.model.RemoteFoodSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class UsdaFoodDataCentralCatalogProviderTest {
    @Test
    fun search_delegatesToUsdaDataSource() = runTest {
        val dataSource = FakeUsdaFoodDataSource()
        val provider = UsdaFoodDataCentralCatalogProvider(dataSource)
        val token = UserInitiatedNetworkGuard().issueToken("search")

        val results = provider.search(
            query = "oats",
            token = token,
        )

        assertEquals("oats", dataSource.lastSearchQuery)
        assertSame(token, dataSource.lastSearchToken)
        assertEquals(dataSource.searchResults, results)
    }

    @Test
    fun lookupBarcode_delegatesToUsdaDataSource() = runTest {
        val dataSource = FakeUsdaFoodDataSource()
        val provider = UsdaFoodDataCentralCatalogProvider(dataSource)
        val token = UserInitiatedNetworkGuard().issueToken("lookup")

        val result = provider.lookupBarcode(
            barcode = "0123456",
            token = token,
        )

        assertEquals("0123456", dataSource.lastLookupBarcode)
        assertSame(token, dataSource.lastLookupToken)
        assertEquals(dataSource.lookupResult, result)
    }
}

private class FakeUsdaFoodDataSource : UsdaFoodDataSource {
    var lastSearchQuery: String? = null
    var lastSearchToken: UserInitiatedNetworkToken? = null
    var lastLookupBarcode: String? = null
    var lastLookupToken: UserInitiatedNetworkToken? = null

    val searchResults = listOf(
        RemoteFoodCandidate(
            source = RemoteFoodSource.USDA_FOODDATA_CENTRAL,
            sourceId = "fdc-123",
            barcode = "0123456",
            name = "Rolled Oats",
            brand = "USDA",
            caloriesKcalPer100g = 379.0,
            proteinGPer100g = 13.2,
            carbsGPer100g = 67.7,
            fatGPer100g = 6.5,
            servingSize = "100 g",
        ),
    )

    val lookupResult = RemoteFoodCandidate(
        source = RemoteFoodSource.USDA_FOODDATA_CENTRAL,
        sourceId = "fdc-999",
        barcode = "0123456",
        name = "Whole Milk",
        brand = "USDA",
        caloriesKcalPer100g = 61.0,
        proteinGPer100g = 3.2,
        carbsGPer100g = 4.8,
        fatGPer100g = 3.3,
        servingSize = "100 g",
    )

    override suspend fun searchByText(
        query: String,
        token: UserInitiatedNetworkToken,
    ): List<RemoteFoodCandidate> {
        lastSearchQuery = query
        lastSearchToken = token
        return searchResults
    }

    override suspend fun lookupByBarcode(
        barcode: String,
        token: UserInitiatedNetworkToken,
    ): RemoteFoodCandidate? {
        lastLookupBarcode = barcode
        lastLookupToken = token
        return lookupResult
    }
}
