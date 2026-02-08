package com.openfuel.app.data.remote

import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.domain.model.RemoteFoodSource
import com.openfuel.app.domain.service.ProviderRequestType
import java.time.Duration
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryProviderResultCacheTest {
    @Test
    fun get_returnsValueBeforeExpiry() = runTest {
        val cache = InMemoryProviderResultCache()
        cache.put(
            cacheKey = "key",
            providerId = "provider",
            requestType = ProviderRequestType.TEXT_SEARCH,
            normalizedInput = "oat",
            items = listOf(candidate("id-1")),
            cachedAtEpochMs = 100L,
            ttl = Duration.ofSeconds(60),
        )

        val cached = cache.get(cacheKey = "key", nowEpochMs = 1_000L)

        assertTrue(cached != null)
        assertEquals(1, cached?.items?.size)
    }

    @Test
    fun get_returnsNullAfterExpiry() = runTest {
        val cache = InMemoryProviderResultCache()
        cache.put(
            cacheKey = "key",
            providerId = "provider",
            requestType = ProviderRequestType.TEXT_SEARCH,
            normalizedInput = "oat",
            items = listOf(candidate("id-1")),
            cachedAtEpochMs = 100L,
            ttl = Duration.ofMillis(10),
        )

        val cached = cache.get(cacheKey = "key", nowEpochMs = 111L)

        assertNull(cached)
    }

    @Test
    fun purgeExpired_removesOnlyExpiredEntries() = runTest {
        val cache = InMemoryProviderResultCache()
        cache.put(
            cacheKey = "expired",
            providerId = "provider",
            requestType = ProviderRequestType.TEXT_SEARCH,
            normalizedInput = "expired",
            items = listOf(candidate("id-expired")),
            cachedAtEpochMs = 100L,
            ttl = Duration.ofMillis(1),
        )
        cache.put(
            cacheKey = "fresh",
            providerId = "provider",
            requestType = ProviderRequestType.TEXT_SEARCH,
            normalizedInput = "fresh",
            items = listOf(candidate("id-fresh")),
            cachedAtEpochMs = 100L,
            ttl = Duration.ofSeconds(100),
        )

        cache.purgeExpired(nowEpochMs = 200L)

        assertNull(cache.get(cacheKey = "expired", nowEpochMs = 200L))
        assertTrue(cache.get(cacheKey = "fresh", nowEpochMs = 200L) != null)
    }

    private fun candidate(sourceId: String): RemoteFoodCandidate {
        return RemoteFoodCandidate(
            source = RemoteFoodSource.STATIC_SAMPLE,
            sourceId = sourceId,
            barcode = null,
            name = "Sample",
            brand = "OpenFuel",
            caloriesKcalPer100g = 100.0,
            proteinGPer100g = 2.0,
            carbsGPer100g = 10.0,
            fatGPer100g = 1.0,
            servingSize = "100 g",
        )
    }
}
