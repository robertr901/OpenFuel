package com.openfuel.app.data.remote

import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.domain.model.RemoteFoodSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class OpenFoodFactsCatalogProviderTest {
    @Test
    fun search_delegatesToRemoteDataSource() = runTest {
        val remoteDataSource = FakeRemoteFoodDataSource()
        val provider = OpenFoodFactsCatalogProvider(remoteDataSource)
        val token = UserInitiatedNetworkGuard().issueToken("search")

        val results = provider.search(query = "oat", token = token)

        assertEquals("oat", remoteDataSource.lastSearchQuery)
        assertSame(token, remoteDataSource.lastSearchToken)
        assertEquals(remoteDataSource.searchResults, results)
    }

    @Test
    fun lookupBarcode_delegatesToRemoteDataSource() = runTest {
        val remoteDataSource = FakeRemoteFoodDataSource()
        val provider = OpenFoodFactsCatalogProvider(remoteDataSource)
        val token = UserInitiatedNetworkGuard().issueToken("lookup")

        val result = provider.lookupBarcode(barcode = "123456", token = token)

        assertEquals("123456", remoteDataSource.lastLookupBarcode)
        assertSame(token, remoteDataSource.lastLookupToken)
        assertEquals(remoteDataSource.lookupResult, result)
    }
}

private class FakeRemoteFoodDataSource : RemoteFoodDataSource {
    var lastSearchQuery: String? = null
    var lastSearchToken: UserInitiatedNetworkToken? = null
    var lastLookupBarcode: String? = null
    var lastLookupToken: UserInitiatedNetworkToken? = null

    val searchResults = listOf(
        RemoteFoodCandidate(
            source = RemoteFoodSource.OPEN_FOOD_FACTS,
            sourceId = "off-1",
            barcode = "123456",
            name = "Oat Drink",
            brand = "Demo",
            caloriesKcalPer100g = 40.0,
            proteinGPer100g = 1.0,
            carbsGPer100g = 6.0,
            fatGPer100g = 1.5,
            servingSize = "100 ml",
        ),
    )

    val lookupResult = RemoteFoodCandidate(
        source = RemoteFoodSource.OPEN_FOOD_FACTS,
        sourceId = "off-2",
        barcode = "654321",
        name = "Greek Yogurt",
        brand = "Demo",
        caloriesKcalPer100g = 60.0,
        proteinGPer100g = 10.0,
        carbsGPer100g = 3.0,
        fatGPer100g = 0.0,
        servingSize = "150 g",
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
