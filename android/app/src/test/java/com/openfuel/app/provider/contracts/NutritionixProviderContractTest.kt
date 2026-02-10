package com.openfuel.app.provider.contracts

import com.openfuel.app.data.remote.NutritionixApi
import com.openfuel.app.data.remote.NutritionixItemLookupResponse
import com.openfuel.app.data.remote.NutritionixNaturalRequest
import com.openfuel.app.data.remote.NutritionixNaturalResponse
import com.openfuel.app.data.remote.NutritionixRemoteFoodDataSource
import com.openfuel.app.data.remote.UserInitiatedNetworkGuard
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionixProviderContractTest : ProviderContractAssertions() {
    @Test
    fun fixtureManifest_includesNutritionixContractCases() {
        val manifest = ProviderFixtureLoader.manifest()
        assertEquals(1, manifest.version)
        assertEquals(
            listOf(
                "search_normal.json",
                "search_edge.json",
                "details_normal.json",
                "details_edge.json",
            ),
            manifest.providers.getValue("nutritionix"),
        )
    }

    @Test
    fun nutritionixSearchFixtures_mapDeterministicallyWithCommonInvariants() = runTest {
        val fixtureNames = ProviderFixtureLoader.manifest().providers.getValue("nutritionix")
            .filter { it.startsWith("search_") }

        fixtureNames.forEach { fixtureName ->
            val response = ProviderFixtureLoader.parseFixture(
                path = "provider_fixtures/nutritionix/$fixtureName",
                clazz = NutritionixNaturalResponse::class.java,
            )
            val guard = UserInitiatedNetworkGuard()
            val dataSource = NutritionixRemoteFoodDataSource(
                api = FakeNutritionixApi(naturalResponse = response),
                userInitiatedNetworkGuard = guard,
                appId = "demo-app-id",
                apiKey = "demo-api-key",
                remoteUserId = "0",
            )

            val first = dataSource.searchByText(
                query = "fixture query",
                token = guard.issueToken("nutritionix_search_$fixtureName"),
            )
            val second = dataSource.searchByText(
                query = "fixture query",
                token = guard.issueToken("nutritionix_search_${fixtureName}_repeat"),
            )

            assertCommonInvariants(
                providerName = "nutritionix",
                fixtureName = fixtureName,
                candidates = first,
            )
            assertDeterministic(
                providerName = "nutritionix",
                fixtureName = fixtureName,
                first = first,
                second = second,
            )

            when (fixtureName) {
                "search_normal.json" -> {
                    assertEquals(2, first.size)
                    val yogurt = first.first { it.sourceId == "nix-123" }
                    assertEquals("Greek Yogurt", yogurt.name)
                    assertEquals("OpenFuel Brand", yogurt.brand)
                    assertEquals("000123456789", yogurt.barcode)
                    assertEquals(60.0, yogurt.caloriesKcalPer100g ?: 0.0, 0.0001)
                    assertEquals(10.0, yogurt.proteinGPer100g ?: 0.0, 0.0001)
                    assertEquals(4.0, yogurt.carbsGPer100g ?: 0.0, 0.0001)
                    assertEquals(1.0, yogurt.fatGPer100g ?: 0.0, 0.0001)
                }

                "search_edge.json" -> {
                    assertEquals(2, first.size)
                    val trailMix = first.first { it.name == "Trail Mix" }
                    assertEquals("UPC-ABC-01", trailMix.sourceId)
                    assertEquals("UPC-ABC-01", trailMix.barcode)
                    assertNull(trailMix.caloriesKcalPer100g)
                    assertEquals(6.0, trailMix.proteinGPer100g ?: 0.0, 0.0001)

                    val oatBites = first.first { it.sourceId == "nix-edge-2" }
                    assertEquals(450.0, oatBites.caloriesKcalPer100g ?: 0.0, 0.0001)
                    assertEquals(24999997.5, oatBites.carbsGPer100g ?: 0.0, 0.0001)
                }
            }
        }
    }

    @Test
    fun nutritionixLookupFixtures_mapDeterministicallyWithProviderSpecificInvariants() = runTest {
        val fixtureNames = ProviderFixtureLoader.manifest().providers.getValue("nutritionix")
            .filter { it.startsWith("details_") }

        fixtureNames.forEach { fixtureName ->
            val response = ProviderFixtureLoader.parseFixture(
                path = "provider_fixtures/nutritionix/$fixtureName",
                clazz = NutritionixItemLookupResponse::class.java,
            )
            val guard = UserInitiatedNetworkGuard()
            val dataSource = NutritionixRemoteFoodDataSource(
                api = FakeNutritionixApi(barcodeResponse = response),
                userInitiatedNetworkGuard = guard,
                appId = "demo-app-id",
                apiKey = "demo-api-key",
                remoteUserId = "0",
            )

            val lookupBarcode = when (fixtureName) {
                "details_normal.json" -> "012345678905"
                else -> "missing-upc"
            }
            val first = dataSource.lookupByBarcode(
                barcode = lookupBarcode,
                token = guard.issueToken("nutritionix_lookup_$fixtureName"),
            )
            val second = dataSource.lookupByBarcode(
                barcode = lookupBarcode,
                token = guard.issueToken("nutritionix_lookup_${fixtureName}_repeat"),
            )

            val firstList = listOfNotNull(first)
            val secondList = listOfNotNull(second)
            assertCommonInvariants(
                providerName = "nutritionix",
                fixtureName = fixtureName,
                candidates = firstList,
            )
            assertDeterministic(
                providerName = "nutritionix",
                fixtureName = fixtureName,
                first = firstList,
                second = secondList,
            )

            when (fixtureName) {
                "details_normal.json" -> {
                    val candidate = requireNotNull(first)
                    assertEquals("nix-cola-1", candidate.sourceId)
                    assertEquals("012345678905", candidate.barcode)
                    assertEquals("Cola Zero", candidate.name)
                    assertEquals(1.4084507, candidate.caloriesKcalPer100g ?: 0.0, 0.0001)
                }

                "details_edge.json" -> {
                    val candidate = requireNotNull(first)
                    assertTrue(candidate.sourceId.startsWith("nutritionix-derived-"))
                    assertNull(candidate.barcode)
                    assertEquals("Energy Bar", candidate.name)
                    assertEquals(475.0, candidate.caloriesKcalPer100g ?: 0.0, 0.0001)
                }
            }
        }
    }
}

private class FakeNutritionixApi(
    private val naturalResponse: NutritionixNaturalResponse = NutritionixNaturalResponse(foods = emptyList()),
    private val barcodeResponse: NutritionixItemLookupResponse = NutritionixItemLookupResponse(foods = emptyList()),
) : NutritionixApi {
    override suspend fun naturalNutrients(
        appId: String,
        apiKey: String,
        remoteUserId: String,
        request: NutritionixNaturalRequest,
    ): NutritionixNaturalResponse {
        return naturalResponse
    }

    override suspend fun lookupByBarcode(
        appId: String,
        apiKey: String,
        remoteUserId: String,
        upc: String,
    ): NutritionixItemLookupResponse {
        return barcodeResponse
    }
}
