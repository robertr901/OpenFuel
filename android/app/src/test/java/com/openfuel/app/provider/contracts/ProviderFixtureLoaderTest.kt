package com.openfuel.app.provider.contracts

import com.openfuel.app.data.remote.OpenFoodFactsSearchResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ProviderFixtureLoaderTest {
    @Test
    fun manifest_listsExpectedProvidersAndVersion() {
        val manifest = ProviderFixtureLoader.manifest()

        assertEquals(1, manifest.version)
        assertTrue(manifest.providers.containsKey("off"))
        assertTrue(manifest.providers.containsKey("usda"))
        assertTrue(manifest.providers.containsKey("nutritionix"))
    }

    @Test
    fun parseFixture_parsesTypedPayload() {
        val response = ProviderFixtureLoader.parseFixture(
            path = "provider_fixtures/off/search_normal.json",
            clazz = OpenFoodFactsSearchResponse::class.java,
        )

        assertEquals(2, response.products?.size)
    }

    @Test
    fun readFixture_missingPathThrowsActionableError() {
        try {
            ProviderFixtureLoader.readFixture("provider_fixtures/off/does_not_exist.json")
            fail("Expected missing fixture to throw")
        } catch (error: IllegalStateException) {
            assertTrue(error.message.orEmpty().contains("Missing fixture resource"))
        }
    }
}
