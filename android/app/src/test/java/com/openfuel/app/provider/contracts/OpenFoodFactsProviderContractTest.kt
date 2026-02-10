package com.openfuel.app.provider.contracts

import com.openfuel.app.data.remote.OpenFoodFactsApi
import com.openfuel.app.data.remote.OpenFoodFactsLookupResponse
import com.openfuel.app.data.remote.OpenFoodFactsRemoteFoodDataSource
import com.openfuel.app.data.remote.OpenFoodFactsSearchResponse
import com.openfuel.app.data.remote.UserInitiatedNetworkGuard
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenFoodFactsProviderContractTest : ProviderContractAssertions() {
    @Test
    fun fixtureManifest_includesOffContractCases() {
        val manifest = ProviderFixtureLoader.manifest()
        assertEquals(1, manifest.version)
        assertTrue(manifest.providers.containsKey("off"))
        assertEquals(
            listOf(
                "search_normal.json",
                "search_edge.json",
                "lookup_normal.json",
                "lookup_edge.json",
            ),
            manifest.providers.getValue("off"),
        )
    }

    @Test
    fun offSearchFixtures_mapDeterministicallyWithCommonInvariants() = runTest {
        val fixtureNames = ProviderFixtureLoader.manifest().providers.getValue("off")
            .filter { it.startsWith("search_") }

        fixtureNames.forEach { fixtureName ->
            val response = ProviderFixtureLoader.parseFixture(
                path = "provider_fixtures/off/$fixtureName",
                clazz = OpenFoodFactsSearchResponse::class.java,
            )
            val guard = UserInitiatedNetworkGuard()
            val dataSource = OpenFoodFactsRemoteFoodDataSource(
                api = FakeOpenFoodFactsApi(searchResponse = response),
                userInitiatedNetworkGuard = guard,
                pageSize = 20,
            )

            val first = dataSource.searchByText(
                query = "fixture query",
                token = guard.issueToken("off_search_$fixtureName"),
            )
            val second = dataSource.searchByText(
                query = "fixture query",
                token = guard.issueToken("off_search_${fixtureName}_repeat"),
            )

            assertCommonInvariants(providerName = "off", fixtureName = fixtureName, candidates = first)
            assertTrustSignals(providerName = "off", fixtureName = fixtureName, candidates = first)
            assertDeterministic(
                providerName = "off",
                fixtureName = fixtureName,
                first = first,
                second = second,
            )

            when (fixtureName) {
                "search_normal.json" -> {
                    assertEquals(2, first.size)
                    val coke = first.first { it.sourceId == "04963406" }
                    assertEquals("Coke Zero", coke.name)
                    assertEquals("The Coca-Cola Company", coke.brand)
                    assertEquals(0.0, coke.caloriesKcalPer100g ?: 0.0, 0.0)
                }

                "search_edge.json" -> {
                    assertEquals(2, first.size)
                    val fiberBar = first.first { it.sourceId == "9001" }
                    assertEquals(180.0, fiberBar.caloriesKcalPer100g ?: 0.0, 0.0)
                    assertEquals("1 bar (40 g)", fiberBar.servingSize)
                    val oatCookies = first.first { it.sourceId == "off-edge-2" }
                    assertEquals("2 biscuits", oatCookies.servingSize)
                    assertTrue(first.none { it.name.isBlank() })
                }
            }
        }
    }

    @Test
    fun offLookupFixtures_mapDeterministicallyWithProviderSpecificInvariants() = runTest {
        val fixtureNames = ProviderFixtureLoader.manifest().providers.getValue("off")
            .filter { it.startsWith("lookup_") }

        fixtureNames.forEach { fixtureName ->
            val response = ProviderFixtureLoader.parseFixture(
                path = "provider_fixtures/off/$fixtureName",
                clazz = OpenFoodFactsLookupResponse::class.java,
            )
            val guard = UserInitiatedNetworkGuard()
            val dataSource = OpenFoodFactsRemoteFoodDataSource(
                api = FakeOpenFoodFactsApi(lookupResponse = response),
                userInitiatedNetworkGuard = guard,
                pageSize = 20,
            )

            val first = dataSource.lookupByBarcode(
                barcode = "fixture-upc",
                token = guard.issueToken("off_lookup_$fixtureName"),
            )
            val second = dataSource.lookupByBarcode(
                barcode = "fixture-upc",
                token = guard.issueToken("off_lookup_${fixtureName}_repeat"),
            )

            val firstList = listOfNotNull(first)
            val secondList = listOfNotNull(second)
            assertCommonInvariants(providerName = "off", fixtureName = fixtureName, candidates = firstList)
            assertTrustSignals(providerName = "off", fixtureName = fixtureName, candidates = firstList)
            assertDeterministic(
                providerName = "off",
                fixtureName = fixtureName,
                first = firstList,
                second = secondList,
            )

            when (fixtureName) {
                "lookup_normal.json" -> {
                    val candidate = requireNotNull(first)
                    assertEquals("04963406", candidate.barcode)
                    assertEquals("Coke Zero", candidate.name)
                    assertEquals(0.0, candidate.caloriesKcalPer100g ?: 0.0, 0.0)
                }

                "lookup_edge.json" -> {
                    val candidate = requireNotNull(first)
                    assertEquals("ABC-123", candidate.barcode)
                    assertEquals("Protein Shake", candidate.name)
                }
            }
        }
    }
}

private class FakeOpenFoodFactsApi(
    private val searchResponse: OpenFoodFactsSearchResponse = OpenFoodFactsSearchResponse(products = emptyList()),
    private val lookupResponse: OpenFoodFactsLookupResponse = OpenFoodFactsLookupResponse(status = 0, product = null),
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
        return lookupResponse
    }
}
