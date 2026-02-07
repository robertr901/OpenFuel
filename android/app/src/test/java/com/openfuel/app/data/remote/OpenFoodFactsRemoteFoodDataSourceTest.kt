package com.openfuel.app.data.remote

import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun searchByText_includesExplicitFieldSelectionInRequest() = runTest {
        val fakeApi = FakeOpenFoodFactsApi(
            searchResponse = Gson().fromJson(
                """{ "products": [] }""",
                OpenFoodFactsSearchResponse::class.java,
            ),
        )
        val guard = UserInitiatedNetworkGuard()
        val dataSource = OpenFoodFactsRemoteFoodDataSource(
            api = fakeApi,
            userInitiatedNetworkGuard = guard,
            pageSize = 10,
        )

        dataSource.searchByText(
            query = "coke",
            token = guard.issueToken("test_search"),
        )

        val fields = fakeApi.lastSearchFields
        assertTrue(fields.contains("product_name"))
        assertTrue(fields.contains("product_name_en"))
        assertTrue(fields.contains("nutriments"))
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

    @Test
    fun searchByText_usesEnglishNameAndTrimsTextFields() = runTest {
        val response = Gson().fromJson(
            """
            {
              "products": [
                {
                  "product_name": " ",
                  "product_name_en": " Sparkling Water ",
                  "brands": "  OpenFuel Brand  ",
                  "serving_size": " 1 can (330 ml) "
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

        val result = dataSource.searchByText(
            query = "sparkling",
            token = guard.issueToken("test_search"),
        ).single()

        assertEquals("Sparkling Water", result.name)
        assertEquals("OpenFuel Brand", result.brand)
        assertEquals("1 can (330 ml)", result.servingSize)
    }

    @Test
    fun searchByText_sanitizesNegativeNutrimentsAndFallsBackSafely() = runTest {
        val response = Gson().fromJson(
            """
            {
              "products": [
                {
                  "id": "nutri-case-1",
                  "product_name": "Test Product",
                  "nutriments": {
                    "energy-kcal_100g": -1.0,
                    "energy-kcal": 120.0,
                    "proteins_100g": -5.0,
                    "proteins": 2.5,
                    "carbohydrates_100g": -3.0,
                    "carbohydrates": -2.0,
                    "fat_100g": 4.0
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

        val result = dataSource.searchByText(
            query = "test",
            token = guard.issueToken("test_search"),
        ).single()

        assertEquals(120.0, result.caloriesKcalPer100g ?: 0.0, 0.0)
        assertEquals(2.5, result.proteinGPer100g ?: 0.0, 0.0)
        assertNull(result.carbsGPer100g)
        assertEquals(4.0, result.fatGPer100g ?: 0.0, 0.0)
    }

    @Test
    fun searchByText_keepsResultWhenNutrimentsAreMissing() = runTest {
        val response = Gson().fromJson(
            """
            {
              "products": [
                {
                  "id": "missing-nutri",
                  "product_name": "No Macro Product"
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

        val result = dataSource.searchByText(
            query = "macro",
            token = guard.issueToken("test_search"),
        ).single()

        assertEquals("No Macro Product", result.name)
        assertNull(result.caloriesKcalPer100g)
        assertNull(result.proteinGPer100g)
        assertNull(result.carbsGPer100g)
        assertNull(result.fatGPer100g)
    }
}

private class FakeOpenFoodFactsApi(
    private val searchResponse: OpenFoodFactsSearchResponse,
) : OpenFoodFactsApi {
    var lastSearchFields: String = ""
        private set

    override suspend fun searchFoods(
        query: String,
        searchSimple: Int,
        action: String,
        json: Int,
        fields: String,
        pageSize: Int,
    ): OpenFoodFactsSearchResponse {
        lastSearchFields = fields
        return searchResponse
    }

    override suspend fun lookupByBarcode(barcode: String): OpenFoodFactsLookupResponse {
        return OpenFoodFactsLookupResponse(
            status = 0,
            product = null,
        )
    }
}
