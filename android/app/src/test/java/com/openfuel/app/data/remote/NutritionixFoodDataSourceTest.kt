package com.openfuel.app.data.remote

import com.google.gson.Gson
import com.openfuel.app.domain.model.RemoteFoodSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionixFoodDataSourceTest {
    @Test
    fun searchByText_mapsNutritionixFieldsIntoCanonicalCandidate() = runTest {
        val response = Gson().fromJson(
            """
            {
              "foods": [
                {
                  "food_name": " Greek Yogurt ",
                  "brand_name": " OpenFuel Brand ",
                  "nix_item_id": "nix-123",
                  "upc": "000123456789",
                  "serving_qty": 1,
                  "serving_unit": "cup",
                  "serving_weight_grams": 200,
                  "nf_calories": 120,
                  "nf_protein": 20,
                  "nf_total_carbohydrate": 8,
                  "nf_total_fat": 2
                }
              ]
            }
            """.trimIndent(),
            NutritionixNaturalResponse::class.java,
        )

        val guard = UserInitiatedNetworkGuard()
        val dataSource = NutritionixRemoteFoodDataSource(
            api = FakeNutritionixApi(
                naturalResponse = response,
                barcodeResponse = NutritionixItemLookupResponse(foods = emptyList()),
            ),
            userInitiatedNetworkGuard = guard,
            appId = "demo-app-id",
            apiKey = "demo-app-key",
        )

        val results = dataSource.searchByText(
            query = "yogurt",
            token = guard.issueToken("test_search"),
        )

        assertEquals(1, results.size)
        val candidate = results.single()
        assertEquals(RemoteFoodSource.NUTRITIONIX, candidate.source)
        assertEquals("nix-123", candidate.sourceId)
        assertEquals("Greek Yogurt", candidate.name)
        assertEquals("OpenFuel Brand", candidate.brand)
        assertEquals("000123456789", candidate.barcode)
        assertEquals("1 cup (200 g)", candidate.servingSize)
        assertEquals(60.0, candidate.caloriesKcalPer100g ?: 0.0, 0.0001)
        assertEquals(10.0, candidate.proteinGPer100g ?: 0.0, 0.0001)
        assertEquals(4.0, candidate.carbsGPer100g ?: 0.0, 0.0001)
        assertEquals(1.0, candidate.fatGPer100g ?: 0.0, 0.0001)
    }

    @Test
    fun searchByText_filtersInvalidRowsAndSanitizesNegativeNutrients() = runTest {
        val response = Gson().fromJson(
            """
            {
              "foods": [
                {
                  "food_name": " ",
                  "nix_item_name": " ",
                  "brand_name": "Ignored"
                },
                {
                  "food_name": "Trail Mix",
                  "brand_name": "Acme",
                  "serving_qty": 40,
                  "serving_unit": "g",
                  "serving_weight_grams": 40,
                  "nf_calories": -200,
                  "nf_protein": 6
                }
              ]
            }
            """.trimIndent(),
            NutritionixNaturalResponse::class.java,
        )

        val guard = UserInitiatedNetworkGuard()
        val dataSource = NutritionixRemoteFoodDataSource(
            api = FakeNutritionixApi(
                naturalResponse = response,
                barcodeResponse = NutritionixItemLookupResponse(foods = emptyList()),
            ),
            userInitiatedNetworkGuard = guard,
            appId = "demo-app-id",
            apiKey = "demo-app-key",
        )

        val results = dataSource.searchByText(
            query = "trail mix",
            token = guard.issueToken("test_search"),
        )

        assertEquals(1, results.size)
        val candidate = results.single()
        assertTrue(candidate.sourceId.startsWith("nutritionix-derived-"))
        assertEquals("Trail Mix", candidate.name)
        assertEquals("Acme", candidate.brand)
        assertNull(candidate.caloriesKcalPer100g)
        assertEquals(15.0, candidate.proteinGPer100g ?: 0.0, 0.0001)
    }

    @Test
    fun lookupByBarcode_prefersExactBarcodeMatch() = runTest {
        val guard = UserInitiatedNetworkGuard()
        val dataSource = NutritionixRemoteFoodDataSource(
            api = FakeNutritionixApi(
                naturalResponse = NutritionixNaturalResponse(foods = emptyList()),
                barcodeResponse = Gson().fromJson(
                    """
                    {
                      "foods": [
                        {
                          "food_name": "Candidate A",
                          "nix_item_id": "a",
                          "upc": "111"
                        },
                        {
                          "food_name": "Candidate B",
                          "nix_item_id": "b",
                          "upc": "999"
                        }
                      ]
                    }
                    """.trimIndent(),
                    NutritionixItemLookupResponse::class.java,
                ),
            ),
            userInitiatedNetworkGuard = guard,
            appId = "demo-app-id",
            apiKey = "demo-app-key",
        )

        val result = dataSource.lookupByBarcode(
            barcode = "999",
            token = guard.issueToken("test_lookup"),
        )

        assertEquals("b", result?.sourceId)
        assertEquals("999", result?.barcode)
    }

    @Test
    fun searchByText_whenCredentialsMissing_returnsEmptyWithoutCallingApi() = runTest {
        val guard = UserInitiatedNetworkGuard()
        val fakeApi = FakeNutritionixApi(
            naturalResponse = NutritionixNaturalResponse(foods = emptyList()),
            barcodeResponse = NutritionixItemLookupResponse(foods = emptyList()),
        )
        val dataSource = NutritionixRemoteFoodDataSource(
            api = fakeApi,
            userInitiatedNetworkGuard = guard,
            appId = "",
            apiKey = "",
        )

        val results = dataSource.searchByText(
            query = "oats",
            token = guard.issueToken("test_search"),
        )

        assertTrue(results.isEmpty())
        assertEquals(0, fakeApi.naturalCalls)
    }
}

private class FakeNutritionixApi(
    private val naturalResponse: NutritionixNaturalResponse,
    private val barcodeResponse: NutritionixItemLookupResponse,
) : NutritionixApi {
    var naturalCalls: Int = 0
        private set

    var barcodeCalls: Int = 0
        private set

    override suspend fun naturalNutrients(
        appId: String,
        apiKey: String,
        remoteUserId: String,
        request: NutritionixNaturalRequest,
    ): NutritionixNaturalResponse {
        naturalCalls += 1
        return naturalResponse
    }

    override suspend fun lookupByBarcode(
        appId: String,
        apiKey: String,
        remoteUserId: String,
        upc: String,
    ): NutritionixItemLookupResponse {
        barcodeCalls += 1
        return barcodeResponse
    }
}
