package com.openfuel.app.data.remote

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.openfuel.app.data.db.ProviderSearchCacheDao
import com.openfuel.app.data.db.ProviderSearchCacheEntity
import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.domain.service.ProviderRequestType
import java.time.Duration

data class CachedProviderResult(
    val items: List<RemoteFoodCandidate>,
    val cachedAtEpochMs: Long,
    val expiresAtEpochMs: Long,
)

interface ProviderResultCache {
    suspend fun get(cacheKey: String, nowEpochMs: Long): CachedProviderResult?

    suspend fun put(
        cacheKey: String,
        providerId: String,
        requestType: ProviderRequestType,
        normalizedInput: String,
        items: List<RemoteFoodCandidate>,
        cachedAtEpochMs: Long,
        ttl: Duration,
    )

    suspend fun purgeExpired(nowEpochMs: Long)
}

class RoomProviderResultCache(
    private val dao: ProviderSearchCacheDao,
    private val gson: Gson = Gson(),
) : ProviderResultCache {
    private val listType = object : TypeToken<List<RemoteFoodCandidate>>() {}.type

    override suspend fun get(cacheKey: String, nowEpochMs: Long): CachedProviderResult? {
        val entity = dao.getByKey(cacheKey) ?: return null
        if (entity.expiresAtEpochMs <= nowEpochMs) {
            dao.deleteByKey(cacheKey)
            return null
        }
        val items = gson.fromJson<List<RemoteFoodCandidate>>(entity.payloadJson, listType)
        return CachedProviderResult(
            items = items,
            cachedAtEpochMs = entity.cachedAtEpochMs,
            expiresAtEpochMs = entity.expiresAtEpochMs,
        )
    }

    override suspend fun put(
        cacheKey: String,
        providerId: String,
        requestType: ProviderRequestType,
        normalizedInput: String,
        items: List<RemoteFoodCandidate>,
        cachedAtEpochMs: Long,
        ttl: Duration,
    ) {
        val expiresAtEpochMs = cachedAtEpochMs + ttl.toMillis()
        val payloadJson = gson.toJson(items, listType)
        dao.upsert(
            ProviderSearchCacheEntity(
                cacheKey = cacheKey,
                providerId = providerId,
                requestType = requestType.name,
                normalizedInput = normalizedInput,
                payloadJson = payloadJson,
                cachedAtEpochMs = cachedAtEpochMs,
                expiresAtEpochMs = expiresAtEpochMs,
                itemCount = items.size,
            ),
        )
    }

    override suspend fun purgeExpired(nowEpochMs: Long) {
        dao.deleteExpired(nowEpochMs)
    }
}

class InMemoryProviderResultCache : ProviderResultCache {
    private val cache = LinkedHashMap<String, CachedProviderResult>()

    override suspend fun get(cacheKey: String, nowEpochMs: Long): CachedProviderResult? {
        val entry = cache[cacheKey] ?: return null
        if (entry.expiresAtEpochMs <= nowEpochMs) {
            cache.remove(cacheKey)
            return null
        }
        return entry
    }

    override suspend fun put(
        cacheKey: String,
        providerId: String,
        requestType: ProviderRequestType,
        normalizedInput: String,
        items: List<RemoteFoodCandidate>,
        cachedAtEpochMs: Long,
        ttl: Duration,
    ) {
        cache[cacheKey] = CachedProviderResult(
            items = items,
            cachedAtEpochMs = cachedAtEpochMs,
            expiresAtEpochMs = cachedAtEpochMs + ttl.toMillis(),
        )
    }

    override suspend fun purgeExpired(nowEpochMs: Long) {
        val expiredKeys = cache.entries
            .filter { (_, value) -> value.expiresAtEpochMs <= nowEpochMs }
            .map { (key, _) -> key }
        expiredKeys.forEach { key ->
            cache.remove(key)
        }
    }
}
