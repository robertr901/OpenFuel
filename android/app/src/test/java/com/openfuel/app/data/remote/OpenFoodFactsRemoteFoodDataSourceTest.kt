package com.openfuel.app.data.remote

import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenFoodFactsRemoteFoodDataSourceTest {
    @Test
    fun searchByText_mapsProductsWhenIdUsesAlternateFieldName() = runTest {
        val response = Gson().fromJson(
            """
            {
              "products": [
                {
                  "id": "off-id-123",
                  "product_name": "Coke Zero",
                  "brands": "Coca-Cola",
                  "serving_size": "330 ml",
                  "nutriments": {
                    "energy-kcal_100g": 0.0,
                    "proteins_100g": 0.0,
                    "carbohydrates_100g": 0.0,
                    "fat_100g": 0.0
                  }
                }
              ]
            }
            """.trimIndent(),
            OpenFoodFactsSearchResponse::class.java,
        )

        val guard = UserInitiatedNetworkGuard()
        val dataSource = OpenFoodFactsRemoteFoodDataSource(
            api = FakeOpenFoodFactsApi(searchResponse = response),
            userInitiatedNetworkGuard = guard,
            pageSize = 10,
        )

        val results = dataSource.searchByText(
            query = "coke zero",
            token = guard.issueToken("test_search"),
        )

        assertEquals(1, results.size)
        assertEquals("off-id-123", results.first().sourceId)
        assertEquals("Coke Zero", results.first().name)
    }

    @Test
    fun searchByText_derivesStableSourceIdWhenNoIdIsProvided() = runTest {
        val response = Gson().fromJson(
            """
            {
              "products": [
                {
                  "product_name": "Mystery Snack",
                  "brands": "Local Brand",
                  "serving_size": "30 g"
                }
              ]
            }
            """.trimIndent(),
            OpenFoodFactsSearchResponse::class.java,
        )

        val guard = UserInitiatedNetworkGuard()
        val dataSource = OpenFoodFactsRemoteFoodDataSource(
            api = FakeOpenFoodFactsApi(searchResponse = response),
            userInitiatedNetworkGuard = guard,
            pageSize = 10,
        )

        val results = dataSource.searchByText(
            query = "mystery snack",
            token = guard.issueToken("test_search"),
        )

        assertEquals(1, results.size)
        assertTrue(results.first().sourceId.startsWith("derived-"))
        assertEquals("Mystery Snack", results.first().name)
    }
}

private class FakeOpenFoodFactsApi(
    private val searchResponse: OpenFoodFactsSearchResponse,
) : OpenFoodFactsApi {
    override suspend fun searchFoods(
        query: String,
        searchSimple: Int,
        action: String,
        json: Int,
        fields: String,
        pageSize: Int,
    ): OpenFoodFactsSearchResponse {
        return searchResponse
    }

    override suspend fun lookupByBarcode(barcode: String): OpenFoodFactsLookupResponse {
        return OpenFoodFactsLookupResponse(
            status = 0,
            product = null,
        )
    }
}
