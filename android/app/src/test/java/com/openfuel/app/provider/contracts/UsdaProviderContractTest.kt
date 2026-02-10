package com.openfuel.app.provider.contracts

import com.openfuel.app.data.remote.UsdaFoodDataCentralApi
import com.openfuel.app.data.remote.UsdaFoodDataCentralDataSource
import com.openfuel.app.data.remote.UsdaFoodSearchRequest
import com.openfuel.app.data.remote.UsdaFoodSearchResponse
import com.openfuel.app.data.remote.UserInitiatedNetworkGuard
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsdaProviderContractTest : ProviderContractAssertions() {
    @Test
    fun fixtureManifest_includesUsdaContractCases() {
        val manifest = ProviderFixtureLoader.manifest()
        assertEquals(1, manifest.version)
        assertEquals(
            listOf(
                "search_normal.json",
                "search_edge.json",
                "details_normal.json",
                "details_edge.json",
            ),
            manifest.providers.getValue("usda"),
        )
    }

    @Test
    fun usdaSearchFixtures_mapDeterministicallyWithCommonInvariants() = runTest {
        val fixtureNames = ProviderFixtureLoader.manifest().providers.getValue("usda")
            .filter { it.startsWith("search_") }

        fixtureNames.forEach { fixtureName ->
            val response = ProviderFixtureLoader.parseFixture(
                path = "provider_fixtures/usda/$fixtureName",
                clazz = UsdaFoodSearchResponse::class.java,
            )
            val guard = UserInitiatedNetworkGuard()
            val dataSource = UsdaFoodDataCentralDataSource(
                api = FakeUsdaFoodDataCentralApi(searchResponse = response),
                userInitiatedNetworkGuard = guard,
                apiKey = "demo-key",
                pageSize = 20,
            )

            val first = dataSource.searchByText(
                query = "fixture query",
                token = guard.issueToken("usda_search_$fixtureName"),
            )
            val second = dataSource.searchByText(
                query = "fixture query",
                token = guard.issueToken("usda_search_${fixtureName}_repeat"),
            )

            assertCommonInvariants(providerName = "usda", fixtureName = fixtureName, candidates = first)
            assertDeterministic(
                providerName = "usda",
                fixtureName = fixtureName,
                first = first,
                second = second,
            )

            when (fixtureName) {
                "search_normal.json" -> {
                    assertEquals(2, first.size)
                    val yogurt = first.first { it.sourceId == "1001" }
                    assertEquals("Greek Yogurt", yogurt.name)
                    assertEquals("OpenFuel Labs", yogurt.brand)
                    assertEquals("000123456789", yogurt.barcode)
                    assertEquals("170 g", yogurt.servingSize)
                    assertEquals(58.8235294, yogurt.caloriesKcalPer100g ?: 0.0, 0.0001)
                    assertEquals(10.0, yogurt.proteinGPer100g ?: 0.0, 0.0001)
                    assertEquals(4.4117647, yogurt.carbsGPer100g ?: 0.0, 0.0001)
                    assertEquals(0.4705882, yogurt.fatGPer100g ?: 0.0, 0.0001)

                    val juice = first.first { it.sourceId == "1002" }
                    assertEquals("240 ml", juice.servingSize)
                }

                "search_edge.json" -> {
                    assertEquals(2, first.size)
                    val biscuit = first.first { it.sourceId == "2002" }
                    assertEquals("Protein Biscuit", biscuit.name)
                    assertEquals(220.0, biscuit.caloriesKcalPer100g ?: 0.0, 0.0)
                    assertNull(biscuit.proteinGPer100g)

                    val trailMix = first.first { it.name == "Trail Mix" }
                    assertTrue(trailMix.sourceId.startsWith("usda-derived-"))
                    assertNull(trailMix.caloriesKcalPer100g)
                }
            }
        }
    }

    @Test
    fun usdaLookupFixtures_mapDeterministicallyWithProviderSpecificInvariants() = runTest {
        val fixtureNames = ProviderFixtureLoader.manifest().providers.getValue("usda")
            .filter { it.startsWith("details_") }

        fixtureNames.forEach { fixtureName ->
            val response = ProviderFixtureLoader.parseFixture(
                path = "provider_fixtures/usda/$fixtureName",
                clazz = UsdaFoodSearchResponse::class.java,
            )
            val guard = UserInitiatedNetworkGuard()
            val dataSource = UsdaFoodDataCentralDataSource(
                api = FakeUsdaFoodDataCentralApi(searchResponse = response),
                userInitiatedNetworkGuard = guard,
                apiKey = "demo-key",
                pageSize = 10,
            )

            val lookupBarcode = when (fixtureName) {
                "details_normal.json" -> "012345678905"
                else -> "not-found-upc"
            }
            val first = dataSource.lookupByBarcode(
                barcode = lookupBarcode,
                token = guard.issueToken("usda_lookup_$fixtureName"),
            )
            val second = dataSource.lookupByBarcode(
                barcode = lookupBarcode,
                token = guard.issueToken("usda_lookup_${fixtureName}_repeat"),
            )

            val firstList = listOfNotNull(first)
            val secondList = listOfNotNull(second)
            assertCommonInvariants(providerName = "usda", fixtureName = fixtureName, candidates = firstList)
            assertDeterministic(
                providerName = "usda",
                fixtureName = fixtureName,
                first = firstList,
                second = secondList,
            )

            when (fixtureName) {
                "details_normal.json" -> {
                    val candidate = requireNotNull(first)
                    assertEquals("3001", candidate.sourceId)
                    assertEquals("012345678905", candidate.barcode)
                    assertEquals("Cola Zero", candidate.name)
                    assertEquals(1.4084507, candidate.caloriesKcalPer100g ?: 0.0, 0.0001)
                }

                "details_edge.json" -> {
                    val candidate = requireNotNull(first)
                    assertNotNull(candidate.sourceId)
                    assertEquals("Unknown Unit Item", candidate.name)
                    assertNull(candidate.barcode)
                    assertEquals(95.0, candidate.caloriesKcalPer100g ?: 0.0, 0.0)
                }
            }
        }
    }
}

private class FakeUsdaFoodDataCentralApi(
    private val searchResponse: UsdaFoodSearchResponse = UsdaFoodSearchResponse(foods = emptyList()),
) : UsdaFoodDataCentralApi {
    override suspend fun searchFoods(
        apiKey: String,
        request: UsdaFoodSearchRequest,
    ): UsdaFoodSearchResponse {
        return searchResponse
    }
}
