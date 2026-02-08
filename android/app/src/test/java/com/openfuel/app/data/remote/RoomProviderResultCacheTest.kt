package com.openfuel.app.data.remote

import com.openfuel.app.data.db.ProviderSearchCacheDao
import com.openfuel.app.data.db.ProviderSearchCacheEntity
import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.domain.model.RemoteFoodSource
import com.openfuel.app.domain.service.ProviderRequestType
import java.time.Duration
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomProviderResultCacheTest {
    @Test
    fun get_staleVersionEntryInvalidatesAsCacheMiss() = runTest {
        val dao = FakeProviderSearchCacheDao()
        dao.upsert(
            ProviderSearchCacheEntity(
                cacheKey = "stale-key",
                providerId = "open_food_facts",
                requestType = ProviderRequestType.TEXT_SEARCH.name,
                normalizedInput = "oat",
                cacheVersion = 0,
                payloadJson = "[]",
                cachedAtEpochMs = 1_000L,
                expiresAtEpochMs = 5_000L,
                itemCount = 0,
            ),
        )
        val cache = RoomProviderResultCache(dao = dao)

        val cached = cache.get(cacheKey = "stale-key", nowEpochMs = 2_000L)

        assertNull(cached)
        assertEquals(0, dao.countAll())
    }

    @Test
    fun get_corruptedPayloadReturnsMissAndNextPutOverwritesSafely() = runTest {
        val dao = FakeProviderSearchCacheDao()
        dao.upsert(
            ProviderSearchCacheEntity(
                cacheKey = "corrupt-key",
                providerId = "open_food_facts",
                requestType = ProviderRequestType.TEXT_SEARCH.name,
                normalizedInput = "banana",
                cacheVersion = PROVIDER_CACHE_PAYLOAD_VERSION,
                payloadJson = "{not-json",
                cachedAtEpochMs = 1_000L,
                expiresAtEpochMs = 10_000L,
                itemCount = 1,
            ),
        )
        val cache = RoomProviderResultCache(dao = dao)

        val first = cache.get(cacheKey = "corrupt-key", nowEpochMs = 2_000L)
        assertNull(first)
        assertEquals(0, dao.countAll())

        cache.put(
            cacheKey = "corrupt-key",
            providerId = "open_food_facts",
            requestType = ProviderRequestType.TEXT_SEARCH,
            normalizedInput = "banana",
            items = listOf(candidate("fresh-1")),
            cachedAtEpochMs = 3_000L,
            ttl = Duration.ofMinutes(30),
        )

        val refreshed = cache.get(cacheKey = "corrupt-key", nowEpochMs = 3_001L)
        assertTrue(refreshed != null)
        assertEquals(1, refreshed?.items?.size)
        assertEquals("fresh-1", refreshed?.items?.first()?.sourceId)
    }

    private fun candidate(sourceId: String): RemoteFoodCandidate {
        return RemoteFoodCandidate(
            source = RemoteFoodSource.OPEN_FOOD_FACTS,
            sourceId = sourceId,
            barcode = null,
            name = "Candidate $sourceId",
            brand = "Brand",
            caloriesKcalPer100g = 100.0,
            proteinGPer100g = 5.0,
            carbsGPer100g = 12.0,
            fatGPer100g = 2.0,
            servingSize = "100 g",
        )
    }
}

private class FakeProviderSearchCacheDao : ProviderSearchCacheDao {
    private val entries = linkedMapOf<String, ProviderSearchCacheEntity>()

    override suspend fun getByKey(cacheKey: String): ProviderSearchCacheEntity? {
        return entries[cacheKey]
    }

    override suspend fun upsert(entry: ProviderSearchCacheEntity) {
        entries[entry.cacheKey] = entry
    }

    override suspend fun deleteByKey(cacheKey: String) {
        entries.remove(cacheKey)
    }

    override suspend fun deleteExpired(nowEpochMs: Long): Int {
        val removed = entries.values
            .filter { entity -> entity.expiresAtEpochMs <= nowEpochMs }
            .map { entity -> entity.cacheKey }
        removed.forEach { cacheKey ->
            entries.remove(cacheKey)
        }
        return removed.size
    }

    override suspend fun deleteByVersionMismatch(expectedVersion: Int): Int {
        val removed = entries.values
            .filter { entity -> entity.cacheVersion != expectedVersion }
            .map { entity -> entity.cacheKey }
        removed.forEach { cacheKey ->
            entries.remove(cacheKey)
        }
        return removed.size
    }

    override suspend fun countAll(): Int {
        return entries.size
    }
}
