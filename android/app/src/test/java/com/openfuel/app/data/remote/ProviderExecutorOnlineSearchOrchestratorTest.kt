package com.openfuel.app.data.remote

import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.domain.model.RemoteFoodSource
import com.openfuel.app.domain.search.OnlineProviderRunStatus
import com.openfuel.app.domain.search.OnlineSearchRequest
import com.openfuel.app.domain.search.SearchSourceFilter
import com.openfuel.app.domain.service.FoodCatalogExecutionProvider
import com.openfuel.app.domain.service.FoodCatalogProvider
import com.openfuel.app.domain.service.FoodCatalogProviderDescriptor
import com.openfuel.app.domain.service.FoodCatalogProviderRegistry
import com.openfuel.app.domain.service.ProviderExecutionReport
import com.openfuel.app.domain.service.ProviderExecutionRequest
import com.openfuel.app.domain.service.ProviderExecutor
import com.openfuel.app.domain.service.ProviderMergedCandidate
import com.openfuel.app.domain.service.ProviderRefreshPolicy
import com.openfuel.app.domain.service.ProviderRequestType
import com.openfuel.app.domain.service.ProviderResult
import com.openfuel.app.domain.service.ProviderStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderExecutorOnlineSearchOrchestratorTest {
    @Test
    fun search_whenOneProviderFails_returnsSuccessfulCandidatesAndBothProviderRuns() = runTest {
        val candidate = remoteCandidate(sourceId = "a-1", name = "Oats")
        val report = ProviderExecutionReport(
            requestType = ProviderRequestType.TEXT_SEARCH,
            sourceFilter = SearchSourceFilter.ONLINE_ONLY,
            mergedCandidates = listOf(
                ProviderMergedCandidate(
                    providerId = "provider_a",
                    candidate = candidate,
                    dedupeKey = "a-1",
                ),
            ),
            providerResults = listOf(
                ProviderResult(
                    providerId = "provider_a",
                    capability = ProviderRequestType.TEXT_SEARCH.capability,
                    status = ProviderStatus.AVAILABLE,
                    items = listOf(candidate),
                    elapsedMs = 12L,
                ),
                ProviderResult(
                    providerId = "provider_b",
                    capability = ProviderRequestType.TEXT_SEARCH.capability,
                    status = ProviderStatus.NETWORK_UNAVAILABLE,
                    items = emptyList(),
                    elapsedMs = 9L,
                    diagnostics = "Network unavailable for provider request.",
                ),
            ),
            overallElapsedMs = 30L,
        )
        val providers = listOf(
            executionProvider(key = "provider_a", displayName = "Provider A", priority = 10),
            executionProvider(key = "provider_b", displayName = "Provider B", priority = 20),
        )
        val orchestrator = ProviderExecutorOnlineSearchOrchestrator(
            providerExecutor = FakeProviderExecutor(report),
            providerRegistry = FakeFoodCatalogProviderRegistry(providers),
        )
        val guard = UserInitiatedNetworkGuard()

        val result = orchestrator.search(
            request = OnlineSearchRequest(
                query = "oat",
                token = guard.issueToken("test_search"),
                onlineLookupEnabled = true,
                refreshPolicy = ProviderRefreshPolicy.CACHE_PREFERRED,
            ),
        )

        assertEquals(1, result.candidates.size)
        assertEquals("provider_a", result.candidates.single().providerKey)
        assertEquals(2, result.providerRuns.size)
        assertEquals(OnlineProviderRunStatus.SUCCESS, result.providerRuns[0].status)
        assertEquals(OnlineProviderRunStatus.FAILED, result.providerRuns[1].status)
        assertEquals(1, result.summary.successfulProviders)
        assertEquals(1, result.summary.failedProviders)
    }

    @Test
    fun search_whenOneProviderReturnsResultsAndAnotherReturnsEmpty_keepsSuccessfulResultsAndStatuses() = runTest {
        val candidate = remoteCandidate(sourceId = "oats-1", name = "Rolled Oats")
        val report = ProviderExecutionReport(
            requestType = ProviderRequestType.TEXT_SEARCH,
            sourceFilter = SearchSourceFilter.ONLINE_ONLY,
            mergedCandidates = listOf(
                ProviderMergedCandidate(
                    providerId = "provider_a",
                    candidate = candidate,
                    dedupeKey = "oats-1",
                ),
            ),
            providerResults = listOf(
                ProviderResult(
                    providerId = "provider_a",
                    capability = ProviderRequestType.TEXT_SEARCH.capability,
                    status = ProviderStatus.AVAILABLE,
                    items = listOf(candidate),
                    elapsedMs = 10L,
                ),
                ProviderResult(
                    providerId = "provider_b",
                    capability = ProviderRequestType.TEXT_SEARCH.capability,
                    status = ProviderStatus.EMPTY,
                    items = emptyList(),
                    elapsedMs = 8L,
                ),
            ),
            overallElapsedMs = 22L,
        )
        val providers = listOf(
            executionProvider(key = "provider_a", displayName = "Provider A", priority = 10),
            executionProvider(key = "provider_b", displayName = "Provider B", priority = 20),
        )
        val orchestrator = ProviderExecutorOnlineSearchOrchestrator(
            providerExecutor = FakeProviderExecutor(report),
            providerRegistry = FakeFoodCatalogProviderRegistry(providers),
        )
        val guard = UserInitiatedNetworkGuard()

        val result = orchestrator.search(
            request = OnlineSearchRequest(
                query = "oats",
                token = guard.issueToken("test_search"),
                onlineLookupEnabled = true,
                refreshPolicy = ProviderRefreshPolicy.CACHE_PREFERRED,
            ),
        )

        assertEquals(1, result.candidates.size)
        assertEquals("provider_a", result.candidates.single().providerKey)
        assertEquals(2, result.providerRuns.size)
        assertEquals(OnlineProviderRunStatus.SUCCESS, result.providerRuns[0].status)
        assertEquals(OnlineProviderRunStatus.EMPTY, result.providerRuns[1].status)
    }

    @Test
    fun search_whenProviderMissingConfig_marksRunAsSkippedMissingConfig() = runTest {
        val report = ProviderExecutionReport(
            requestType = ProviderRequestType.TEXT_SEARCH,
            sourceFilter = SearchSourceFilter.ONLINE_ONLY,
            mergedCandidates = emptyList(),
            providerResults = listOf(
                ProviderResult(
                    providerId = "usda_fdc",
                    capability = ProviderRequestType.TEXT_SEARCH.capability,
                    status = ProviderStatus.DISABLED_BY_SETTINGS,
                    items = emptyList(),
                    elapsedMs = 0L,
                    diagnostics = "USDA API key missing. Add USDA_API_KEY in local.properties.",
                ),
            ),
            overallElapsedMs = 4L,
        )
        val providers = listOf(
            executionProvider(key = "usda_fdc", displayName = "USDA FoodData Central", priority = 10),
        )
        val orchestrator = ProviderExecutorOnlineSearchOrchestrator(
            providerExecutor = FakeProviderExecutor(report),
            providerRegistry = FakeFoodCatalogProviderRegistry(providers),
        )
        val guard = UserInitiatedNetworkGuard()

        val result = orchestrator.search(
            request = OnlineSearchRequest(
                query = "chicken",
                token = guard.issueToken("test_search"),
                onlineLookupEnabled = true,
                refreshPolicy = ProviderRefreshPolicy.CACHE_PREFERRED,
            ),
        )

        assertEquals(1, result.providerRuns.size)
        assertEquals(OnlineProviderRunStatus.SKIPPED_MISSING_CONFIG, result.providerRuns.single().status)
        assertTrue(result.providerRuns.single().message.orEmpty().contains("API key"))
        assertEquals(1, result.summary.skippedProviders)
    }

    @Test
    fun search_whenDuplicateCandidatesFromProviders_prefersRicherPayload() = runTest {
        val providerACandidate = RemoteFoodCandidate(
            source = RemoteFoodSource.OPEN_FOOD_FACTS,
            sourceId = "off-1",
            providerKey = null,
            barcode = "0123456789",
            name = "Protein Bar",
            brand = "Acme",
            caloriesKcalPer100g = 380.0,
            proteinGPer100g = null,
            carbsGPer100g = null,
            fatGPer100g = null,
            servingSize = "100 g",
        )
        val providerBCandidate = RemoteFoodCandidate(
            source = RemoteFoodSource.NUTRITIONIX,
            sourceId = "nix-1",
            providerKey = null,
            barcode = "0123456789",
            name = "Protein Bar",
            brand = "Acme",
            caloriesKcalPer100g = 380.0,
            proteinGPer100g = 25.0,
            carbsGPer100g = 45.0,
            fatGPer100g = 10.0,
            servingSize = "100 g",
        )
        val report = ProviderExecutionReport(
            requestType = ProviderRequestType.TEXT_SEARCH,
            sourceFilter = SearchSourceFilter.ONLINE_ONLY,
            mergedCandidates = listOf(
                ProviderMergedCandidate(
                    providerId = "provider_a",
                    candidate = providerACandidate,
                    dedupeKey = "barcode:0123456789",
                ),
            ),
            providerResults = listOf(
                ProviderResult(
                    providerId = "provider_a",
                    capability = ProviderRequestType.TEXT_SEARCH.capability,
                    status = ProviderStatus.AVAILABLE,
                    items = listOf(providerACandidate),
                    elapsedMs = 11L,
                ),
                ProviderResult(
                    providerId = "provider_b",
                    capability = ProviderRequestType.TEXT_SEARCH.capability,
                    status = ProviderStatus.AVAILABLE,
                    items = listOf(providerBCandidate),
                    elapsedMs = 12L,
                ),
            ),
            overallElapsedMs = 24L,
        )
        val providers = listOf(
            executionProvider(key = "provider_a", displayName = "Provider A", priority = 10),
            executionProvider(key = "provider_b", displayName = "Provider B", priority = 20),
        )
        val orchestrator = ProviderExecutorOnlineSearchOrchestrator(
            providerExecutor = FakeProviderExecutor(report),
            providerRegistry = FakeFoodCatalogProviderRegistry(providers),
        )
        val guard = UserInitiatedNetworkGuard()

        val result = orchestrator.search(
            request = OnlineSearchRequest(
                query = "bar",
                token = guard.issueToken("test_search"),
                onlineLookupEnabled = true,
                refreshPolicy = ProviderRefreshPolicy.CACHE_PREFERRED,
            ),
        )

        assertEquals(1, result.candidates.size)
        val merged = result.candidates.single()
        assertEquals("provider_b", merged.providerKey)
        assertEquals(RemoteFoodSource.NUTRITIONIX, merged.source)
        assertEquals(25.0, merged.proteinGPer100g ?: 0.0, 0.0)
    }

    @Test
    fun search_whenProviderReturnsEmpty_marksRunAsEmpty() = runTest {
        val report = ProviderExecutionReport(
            requestType = ProviderRequestType.TEXT_SEARCH,
            sourceFilter = SearchSourceFilter.ONLINE_ONLY,
            mergedCandidates = emptyList(),
            providerResults = listOf(
                ProviderResult(
                    providerId = "open_food_facts",
                    capability = ProviderRequestType.TEXT_SEARCH.capability,
                    status = ProviderStatus.EMPTY,
                    items = emptyList(),
                    elapsedMs = 7L,
                ),
            ),
            overallElapsedMs = 10L,
        )
        val providers = listOf(
            executionProvider(key = "open_food_facts", displayName = "Open Food Facts", priority = 10),
        )
        val orchestrator = ProviderExecutorOnlineSearchOrchestrator(
            providerExecutor = FakeProviderExecutor(report),
            providerRegistry = FakeFoodCatalogProviderRegistry(providers),
        )
        val guard = UserInitiatedNetworkGuard()

        val result = orchestrator.search(
            request = OnlineSearchRequest(
                query = "unknown",
                token = guard.issueToken("test_search"),
                onlineLookupEnabled = true,
                refreshPolicy = ProviderRefreshPolicy.CACHE_PREFERRED,
            ),
        )

        assertTrue(result.candidates.isEmpty())
        assertEquals(OnlineProviderRunStatus.EMPTY, result.providerRuns.single().status)
        assertEquals(1, result.summary.successfulProviders)
        assertEquals(0, result.summary.failedProviders)
    }
}

private class FakeProviderExecutor(
    private val report: ProviderExecutionReport,
) : ProviderExecutor {
    override suspend fun execute(request: ProviderExecutionRequest): ProviderExecutionReport {
        return report
    }
}

private class FakeFoodCatalogProviderRegistry(
    private val providers: List<FoodCatalogExecutionProvider>,
) : FoodCatalogProviderRegistry {
    override fun providersFor(
        requestType: ProviderRequestType,
        onlineLookupEnabled: Boolean,
    ): List<FoodCatalogExecutionProvider> {
        return providers
    }

    override fun primaryTextSearchProvider(): FoodCatalogProvider {
        error("Not required for these tests.")
    }

    override fun providerDiagnostics(onlineLookupEnabled: Boolean): List<FoodCatalogProviderDescriptor> {
        return providers.map { executionProvider -> executionProvider.descriptor }
    }
}

private fun executionProvider(
    key: String,
    displayName: String,
    priority: Int,
): FoodCatalogExecutionProvider {
    return FoodCatalogExecutionProvider(
        descriptor = FoodCatalogProviderDescriptor(
            key = key,
            displayName = displayName,
            priority = priority,
            supportsBarcode = true,
            supportsTextSearch = true,
            termsOfUseLink = null,
            enabled = true,
            statusReason = "Configured.",
        ),
        provider = null,
    )
}

private fun remoteCandidate(
    sourceId: String,
    name: String,
): RemoteFoodCandidate {
    return RemoteFoodCandidate(
        source = RemoteFoodSource.STATIC_SAMPLE,
        sourceId = sourceId,
        providerKey = null,
        barcode = null,
        name = name,
        brand = null,
        caloriesKcalPer100g = 100.0,
        proteinGPer100g = 5.0,
        carbsGPer100g = 10.0,
        fatGPer100g = 2.0,
        servingSize = "100 g",
    )
}
