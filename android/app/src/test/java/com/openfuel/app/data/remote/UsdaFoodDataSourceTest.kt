package com.openfuel.app.data.remote

import com.openfuel.app.domain.model.RemoteFoodSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsdaFoodDataSourceTest {
    @Test
    fun searchByText_mapsFdcFieldsIntoCanonicalCandidate() = runTest {
        val guard = UserInitiatedNetworkGuard()
        val dataSource = UsdaFoodDataCentralDataSource(
            api = FakeUsdaFoodDataCentralApi(
                searchResponse = UsdaFoodSearchResponse(
                    foods = listOf(
                        UsdaFoodDto(
                            fdcId = 12345L,
                            description = " Greek Yogurt ",
                            brandOwner = " OpenFuel Labs ",
                            brandName = null,
                            gtinUpc = "000123456789",
                            servingSize = 100.0,
                            servingSizeUnit = "g",
                            foodNutrients = listOf(
                                UsdaFoodNutrientDto(
                                    nutrientNumber = "1008",
                                    nutrientName = "Energy",
                                    value = 59.0,
                                ),
                                UsdaFoodNutrientDto(
                                    nutrientNumber = "1003",
                                    nutrientName = "Protein",
                                    value = 10.3,
                                ),
                                UsdaFoodNutrientDto(
                                    nutrientNumber = "1005",
                                    nutrientName = "Carbohydrate",
                                    value = 3.6,
                                ),
                                UsdaFoodNutrientDto(
                                    nutrientNumber = "1004",
                                    nutrientName = "Total lipid (fat)",
                                    value = 0.4,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            userInitiatedNetworkGuard = guard,
            apiKey = "demo-key",
            pageSize = 20,
        )

        val results = dataSource.searchByText(
            query = "yogurt",
            token = guard.issueToken("test_search"),
        )

        assertEquals(1, results.size)
        val candidate = results.single()
        assertEquals(RemoteFoodSource.USDA_FOODDATA_CENTRAL, candidate.source)
        assertEquals("12345", candidate.sourceId)
        assertEquals("Greek Yogurt", candidate.name)
        assertEquals("OpenFuel Labs", candidate.brand)
        assertEquals("000123456789", candidate.barcode)
        assertEquals("100 g", candidate.servingSize)
        assertEquals(59.0, candidate.caloriesKcalPer100g ?: 0.0, 0.0)
        assertEquals(10.3, candidate.proteinGPer100g ?: 0.0, 0.0)
        assertEquals(3.6, candidate.carbsGPer100g ?: 0.0, 0.0)
        assertEquals(0.4, candidate.fatGPer100g ?: 0.0, 0.0)
    }

    @Test
    fun searchByText_safelyFiltersInvalidRowsAndNutrients() = runTest {
        val guard = UserInitiatedNetworkGuard()
        val dataSource = UsdaFoodDataCentralDataSource(
            api = FakeUsdaFoodDataCentralApi(
                searchResponse = UsdaFoodSearchResponse(
                    foods = listOf(
                        UsdaFoodDto(
                            fdcId = 1L,
                            description = " ",
                            brandOwner = null,
                            brandName = null,
                            gtinUpc = null,
                            servingSize = null,
                            servingSizeUnit = null,
                            foodNutrients = emptyList(),
                        ),
                        UsdaFoodDto(
                            fdcId = null,
                            description = "Trail Mix",
                            brandOwner = "Acme",
                            brandName = null,
                            gtinUpc = null,
                            servingSize = 40.0,
                            servingSizeUnit = "g",
                            foodNutrients = listOf(
                                UsdaFoodNutrientDto(
                                    nutrientNumber = "1008",
                                    nutrientName = "Energy",
                                    value = -100.0,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            userInitiatedNetworkGuard = guard,
            apiKey = "demo-key",
            pageSize = 20,
        )

        val results = dataSource.searchByText(
            query = "trail",
            token = guard.issueToken("test_search"),
        )

        assertEquals(1, results.size)
        val candidate = results.single()
        assertTrue(candidate.sourceId.startsWith("usda-derived-"))
        assertEquals("Trail Mix", candidate.name)
        assertNull(candidate.caloriesKcalPer100g)
    }

    @Test
    fun lookupByBarcode_prefersExactBarcodeMatch() = runTest {
        val guard = UserInitiatedNetworkGuard()
        val dataSource = UsdaFoodDataCentralDataSource(
            api = FakeUsdaFoodDataCentralApi(
                searchResponse = UsdaFoodSearchResponse(
                    foods = listOf(
                        UsdaFoodDto(
                            fdcId = 777L,
                            description = "Candidate A",
                            brandOwner = null,
                            brandName = null,
                            gtinUpc = "111",
                            servingSize = null,
                            servingSizeUnit = null,
                            foodNutrients = emptyList(),
                        ),
                        UsdaFoodDto(
                            fdcId = 888L,
                            description = "Candidate B",
                            brandOwner = null,
                            brandName = null,
                            gtinUpc = "999",
                            servingSize = null,
                            servingSizeUnit = null,
                            foodNutrients = emptyList(),
                        ),
                    ),
                ),
            ),
            userInitiatedNetworkGuard = guard,
            apiKey = "demo-key",
            pageSize = 20,
        )

        val result = dataSource.lookupByBarcode(
            barcode = "999",
            token = guard.issueToken("test_lookup"),
        )

        assertEquals("888", result?.sourceId)
        assertEquals("999", result?.barcode)
    }

    @Test
    fun searchByText_whenApiKeyMissing_returnsEmptyWithoutCallingApi() = runTest {
        val guard = UserInitiatedNetworkGuard()
        val fakeApi = FakeUsdaFoodDataCentralApi(
            searchResponse = UsdaFoodSearchResponse(foods = emptyList()),
        )
        val dataSource = UsdaFoodDataCentralDataSource(
            api = fakeApi,
            userInitiatedNetworkGuard = guard,
            apiKey = "",
            pageSize = 20,
        )

        val results = dataSource.searchByText(
            query = "oats",
            token = guard.issueToken("test_search"),
        )

        assertTrue(results.isEmpty())
        assertEquals(0, fakeApi.searchCalls)
    }
}

private class FakeUsdaFoodDataCentralApi(
    private val searchResponse: UsdaFoodSearchResponse,
) : UsdaFoodDataCentralApi {
    var searchCalls: Int = 0
        private set

    override suspend fun searchFoods(
        apiKey: String,
        request: UsdaFoodSearchRequest,
    ): UsdaFoodSearchResponse {
        searchCalls += 1
        return searchResponse
    }
}
