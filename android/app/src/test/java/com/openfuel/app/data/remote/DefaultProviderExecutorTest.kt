package com.openfuel.app.data.remote

import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.domain.model.RemoteFoodSource
import com.openfuel.app.domain.search.SearchSourceFilter
import com.openfuel.app.domain.service.FoodCatalogProvider
import com.openfuel.app.domain.service.FoodCatalogExecutionProvider
import com.openfuel.app.domain.service.FoodCatalogProviderDescriptor
import com.openfuel.app.domain.service.ProviderExecutionPolicy
import com.openfuel.app.domain.service.ProviderExecutionRequest
import com.openfuel.app.domain.service.ProviderRequestType
import com.openfuel.app.domain.service.ProviderStatus
import com.openfuel.app.domain.service.buildProviderCacheKey
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultProviderExecutorTest {
    private val guard = UserInitiatedNetworkGuard()

    @Test
    fun execute_singleProviderSuccess_returnsAvailableResults() = runTest {
        val provider = FakeExecutorFoodCatalogProvider(
            searchResults = listOf(
                candidate(sourceId = "oat-1", barcode = "111", name = "Oatmeal", brand = "A", servingSize = "40 g"),
            ),
        )
        val executor = DefaultProviderExecutor(
            providerSource = { _ ->
                listOf(
                    provider(
                        key = "provider_a",
                        priority = 10,
                        provider = provider,
                    ),
                )
            },
        )

        val report = executor.execute(
            request = textRequest(
                query = "oat",
                token = guard.issueToken("search_online"),
            ),
        )

        assertEquals(1, report.providerResults.size)
        assertEquals(ProviderStatus.AVAILABLE, report.providerResults.single().status)
        assertEquals(1, report.mergedCandidates.size)
        assertEquals("provider_a", report.mergedCandidates.single().providerId)
        assertEquals(1, provider.searchCalls)
    }

    @Test
    fun execute_multipleProviders_dedupesByCanonicalKeyAndKeepsPriorityOrder() = runTest {
        val highPriorityProvider = FakeExecutorFoodCatalogProvider(
            searchResults = listOf(
                candidate(sourceId = "oat-1", barcode = "111", name = "Oatmeal", brand = "A", servingSize = "40 g"),
                candidate(sourceId = "milk-1", barcode = null, name = "Almond  Milk", brand = "Brand", servingSize = "250 ml"),
            ),
        )
        val lowerPriorityProvider = FakeExecutorFoodCatalogProvider(
            searchResults = listOf(
                candidate(sourceId = "dup-barcode", barcode = "111", name = "Duplicate", brand = "Other", servingSize = "100 g"),
                candidate(sourceId = "banana-1", barcode = null, name = "Banana", brand = "Fresh", servingSize = "100 g"),
            ),
        )
        val executor = DefaultProviderExecutor(
            providerSource = { _ ->
                listOf(
                    provider(key = "p2", priority = 20, provider = lowerPriorityProvider),
                    provider(key = "p1", priority = 10, provider = highPriorityProvider),
                )
            },
        )

        val report = executor.execute(
            request = textRequest(
                query = "mix",
                token = guard.issueToken("search_online"),
            ),
        )

        assertEquals(3, report.mergedCandidates.size)
        assertEquals("p1", report.mergedCandidates[0].providerId)
        assertEquals("p1", report.mergedCandidates[1].providerId)
        assertEquals("p2", report.mergedCandidates[2].providerId)
        assertEquals("oat-1", report.mergedCandidates[0].candidate.sourceId)
        assertEquals("milk-1", report.mergedCandidates[1].candidate.sourceId)
        assertEquals("banana-1", report.mergedCandidates[2].candidate.sourceId)
    }

    @Test
    fun execute_providerTimeout_returnsTimeoutStatusWithoutThrowing() = runTest {
        val slowProvider = FakeExecutorFoodCatalogProvider(
            delayMs = 2_000L,
            searchResults = listOf(
                candidate(sourceId = "slow", barcode = "222", name = "Slow Food", brand = null, servingSize = null),
            ),
        )
        val executor = DefaultProviderExecutor(
            providerSource = { _ -> listOf(provider(key = "slow", priority = 10, provider = slowProvider)) },
            policy = ProviderExecutionPolicy(
                overallTimeout = Duration.ofSeconds(3),
                perProviderTimeout = Duration.ofMillis(250),
            ),
        )

        val report = executor.execute(
            request = textRequest(
                query = "slow",
                token = guard.issueToken("search_online"),
            ),
        )

        assertEquals(ProviderStatus.TIMEOUT, report.providerResults.single().status)
        assertTrue(report.mergedCandidates.isEmpty())
    }

    @Test
    fun execute_localOnlyMarksAllProvidersDisabledAndSkipsExecution() = runTest {
        val provider = FakeExecutorFoodCatalogProvider(
            searchResults = listOf(
                candidate(sourceId = "x", barcode = "999", name = "Hidden", brand = null, servingSize = null),
            ),
        )
        val executor = DefaultProviderExecutor(
            providerSource = { _ -> listOf(provider(key = "provider_a", priority = 10, provider = provider)) },
        )

        val report = executor.execute(
            request = ProviderExecutionRequest(
                requestType = ProviderRequestType.TEXT_SEARCH,
                sourceFilter = SearchSourceFilter.LOCAL_ONLY,
                query = "hidden",
                token = guard.issueToken("search_online"),
            ),
        )

        assertEquals(ProviderStatus.DISABLED_BY_SOURCE_FILTER, report.providerResults.single().status)
        assertEquals(0, provider.searchCalls)
    }

    @Test
    fun execute_missingTokenMarksGuardRejectedAndSkipsExecution() = runTest {
        val provider = FakeExecutorFoodCatalogProvider()
        val executor = DefaultProviderExecutor(
            providerSource = { _ -> listOf(provider(key = "provider_a", priority = 10, provider = provider)) },
        )

        val report = executor.execute(
            request = textRequest(
                query = "oat",
                token = null,
            ),
        )

        assertEquals(ProviderStatus.GUARD_REJECTED, report.providerResults.single().status)
        assertEquals(0, provider.searchCalls)
    }

    @Test
    fun execute_providerFailureIsReturnedAsStructuredError() = runTest {
        val failingProvider = FakeExecutorFoodCatalogProvider(
            throwable = IllegalStateException("boom"),
        )
        val executor = DefaultProviderExecutor(
            providerSource = { _ -> listOf(provider(key = "provider_a", priority = 10, provider = failingProvider)) },
        )

        val report = executor.execute(
            request = textRequest(
                query = "oat",
                token = guard.issueToken("search_online"),
            ),
        )

        assertEquals(ProviderStatus.ERROR, report.providerResults.single().status)
        assertTrue(report.mergedCandidates.isEmpty())
    }

    @Test
    fun execute_cacheHitReturnsImmediatelyWithoutProviderCall() = runTest {
        val provider = FakeExecutorFoodCatalogProvider(
            searchResults = listOf(
                candidate(sourceId = "network", barcode = "777", name = "Network Food", brand = null, servingSize = null),
            ),
        )
        val cache = InMemoryProviderResultCache()
        val now = 1_000_000L
        val cacheKey = buildProviderCacheKey(
            providerId = "provider_a",
            requestType = ProviderRequestType.TEXT_SEARCH,
            rawInput = "oat",
        )
        cache.put(
            cacheKey = cacheKey,
            providerId = "provider_a",
            requestType = ProviderRequestType.TEXT_SEARCH,
            normalizedInput = "oat",
            items = listOf(
                candidate(sourceId = "cached", barcode = "123", name = "Cached Oatmeal", brand = "Cache", servingSize = "40 g"),
            ),
            cachedAtEpochMs = now,
            ttl = Duration.ofHours(1),
        )
        val clock = ExecutorMutableClock(Instant.ofEpochMilli(now))
        val executor = DefaultProviderExecutor(
            providerSource = { _ -> listOf(provider(key = "provider_a", priority = 10, provider = provider)) },
            cache = cache,
            clock = clock,
        )

        val report = executor.execute(
            request = textRequest(
                query = "oat",
                token = null,
            ),
        )

        assertEquals(1, report.mergedCandidates.size)
        assertEquals("cached", report.mergedCandidates.single().candidate.sourceId)
        assertEquals(0, provider.searchCalls)
        assertEquals(1, report.cacheStats.hitCount)
    }

    @Test
    fun execute_cacheMissStoresAndSubsequentRequestHitsCache() = runTest {
        val provider = FakeExecutorFoodCatalogProvider(
            searchResults = listOf(
                candidate(sourceId = "network", barcode = "777", name = "Network Food", brand = null, servingSize = null),
            ),
        )
        val cache = InMemoryProviderResultCache()
        val clock = ExecutorMutableClock(Instant.ofEpochMilli(2_000_000L))
        val executor = DefaultProviderExecutor(
            providerSource = { _ -> listOf(provider(key = "provider_a", priority = 10, provider = provider)) },
            cache = cache,
            clock = clock,
        )

        val first = executor.execute(
            request = textRequest(
                query = "network",
                token = guard.issueToken("search_online"),
            ),
        )
        clock.currentInstant = Instant.ofEpochMilli(2_000_500L)
        val second = executor.execute(
            request = textRequest(
                query = "network",
                token = null,
            ),
        )

        assertEquals(1, provider.searchCalls)
        assertEquals(0, first.cacheStats.hitCount)
        assertEquals(1, second.cacheStats.hitCount)
        assertTrue(second.providerResults.single().fromCache)
    }

    @Test
    fun execute_expiredCacheEntryFallsBackToProvider() = runTest {
        val provider = FakeExecutorFoodCatalogProvider(
            searchResults = listOf(
                candidate(sourceId = "fresh", barcode = "888", name = "Fresh Food", brand = null, servingSize = null),
            ),
        )
        val cache = InMemoryProviderResultCache()
        val cacheKey = buildProviderCacheKey(
            providerId = "provider_a",
            requestType = ProviderRequestType.TEXT_SEARCH,
            rawInput = "fresh",
        )
        cache.put(
            cacheKey = cacheKey,
            providerId = "provider_a",
            requestType = ProviderRequestType.TEXT_SEARCH,
            normalizedInput = "fresh",
            items = listOf(
                candidate(sourceId = "expired", barcode = "999", name = "Expired", brand = null, servingSize = null),
            ),
            cachedAtEpochMs = 1_000L,
            ttl = Duration.ofMillis(10),
        )
        val clock = ExecutorMutableClock(Instant.ofEpochMilli(2_000L))
        val executor = DefaultProviderExecutor(
            providerSource = { _ -> listOf(provider(key = "provider_a", priority = 10, provider = provider)) },
            cache = cache,
            clock = clock,
        )

        val report = executor.execute(
            request = textRequest(
                query = "fresh",
                token = guard.issueToken("search_online"),
            ),
        )

        assertEquals(1, provider.searchCalls)
        assertEquals("fresh", report.mergedCandidates.single().candidate.sourceId)
        assertEquals(0, report.cacheStats.hitCount)
    }

    private fun textRequest(
        query: String,
        token: UserInitiatedNetworkToken?,
    ): ProviderExecutionRequest {
        return ProviderExecutionRequest(
            requestType = ProviderRequestType.TEXT_SEARCH,
            sourceFilter = SearchSourceFilter.ONLINE_ONLY,
            query = query,
            token = token,
        )
    }

    private fun provider(
        key: String,
        priority: Int,
        provider: FoodCatalogProvider?,
        enabled: Boolean = true,
    ): FoodCatalogExecutionProvider {
        return FoodCatalogExecutionProvider(
            descriptor = FoodCatalogProviderDescriptor(
                key = key,
                displayName = key,
                priority = priority,
                supportsBarcode = true,
                supportsTextSearch = true,
                termsOfUseLink = null,
                enabled = enabled,
                statusReason = if (enabled) "Enabled" else "Disabled",
            ),
            provider = provider,
        )
    }

    private fun candidate(
        sourceId: String,
        barcode: String?,
        name: String,
        brand: String?,
        servingSize: String?,
    ): RemoteFoodCandidate {
        return RemoteFoodCandidate(
            source = RemoteFoodSource.OPEN_FOOD_FACTS,
            sourceId = sourceId,
            barcode = barcode,
            name = name,
            brand = brand,
            caloriesKcalPer100g = 100.0,
            proteinGPer100g = 2.0,
            carbsGPer100g = 10.0,
            fatGPer100g = 1.0,
            servingSize = servingSize,
        )
    }
}

private class ExecutorMutableClock(
    var currentInstant: Instant,
) : Clock() {
    override fun getZone(): ZoneId = ZoneId.of("UTC")

    override fun withZone(zone: ZoneId): Clock = this

    override fun instant(): Instant = currentInstant
}

private class FakeExecutorFoodCatalogProvider(
    private val searchResults: List<RemoteFoodCandidate> = emptyList(),
    private val lookupResult: RemoteFoodCandidate? = null,
    private val delayMs: Long = 0L,
    private val throwable: Throwable? = null,
) : FoodCatalogProvider {
    var searchCalls: Int = 0
    var barcodeCalls: Int = 0

    override suspend fun search(
        query: String,
        token: UserInitiatedNetworkToken,
    ): List<RemoteFoodCandidate> {
        searchCalls += 1
        if (delayMs > 0) {
            delay(delayMs)
        }
        throwable?.let { throw it }
        return searchResults
    }

    override suspend fun lookupBarcode(
        barcode: String,
        token: UserInitiatedNetworkToken,
    ): RemoteFoodCandidate? {
        barcodeCalls += 1
        if (delayMs > 0) {
            delay(delayMs)
        }
        throwable?.let { throw it }
        return lookupResult
    }
}
