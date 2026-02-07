package com.openfuel.app.data.remote

import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.domain.search.SearchSourceFilter
import com.openfuel.app.domain.service.FoodCatalogProvider
import com.openfuel.app.domain.service.FoodCatalogProviderDescriptor
import com.openfuel.app.domain.service.ProviderCapability
import com.openfuel.app.domain.service.ProviderExecutionPolicy
import com.openfuel.app.domain.service.ProviderExecutionReport
import com.openfuel.app.domain.service.ProviderExecutionRequest
import com.openfuel.app.domain.service.ProviderExecutor
import com.openfuel.app.domain.service.ProviderMergedCandidate
import com.openfuel.app.domain.service.ProviderRequestType
import com.openfuel.app.domain.service.ProviderResult
import com.openfuel.app.domain.service.ProviderStatus
import com.openfuel.app.domain.service.buildProviderDedupeKey
import java.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Dispatchers

class DefaultProviderExecutor(
    private val providerSource: () -> List<ExecutableFoodCatalogProvider>,
    private val policy: ProviderExecutionPolicy = ProviderExecutionPolicy(),
    private val clock: Clock = Clock.systemUTC(),
) : ProviderExecutor {
    override suspend fun execute(request: ProviderExecutionRequest): ProviderExecutionReport {
        return withContext(Dispatchers.IO) {
            val startedAtMs = clock.millis()
            val providers = providerSource()
                .sortedWith(compareBy<ExecutableFoodCatalogProvider> { it.metadata.priority }.thenBy { it.metadata.key })

            if (providers.isEmpty()) {
                return@withContext ProviderExecutionReport(
                    requestType = request.requestType,
                    sourceFilter = request.sourceFilter,
                    mergedCandidates = emptyList(),
                    providerResults = emptyList(),
                    overallElapsedMs = elapsedSince(startedAtMs),
                )
            }

            val providerResults = if (request.sourceFilter == SearchSourceFilter.LOCAL_ONLY) {
                providers.map { provider ->
                    disabledResult(
                        provider = provider,
                        requestType = request.requestType,
                        status = ProviderStatus.DISABLED_BY_SOURCE_FILTER,
                        diagnostics = "Online providers skipped due to local-only source filter.",
                    )
                }
            } else {
                executeProviders(providers = providers, request = request)
            }

            val mergedCandidates = mergeCandidates(providerResults)
            ProviderExecutionReport(
                requestType = request.requestType,
                sourceFilter = request.sourceFilter,
                mergedCandidates = mergedCandidates,
                providerResults = providerResults,
                overallElapsedMs = elapsedSince(startedAtMs),
            )
        }
    }

    private suspend fun executeProviders(
        providers: List<ExecutableFoodCatalogProvider>,
        request: ProviderExecutionRequest,
    ): List<ProviderResult> {
        val overallTimeoutMs = policy.overallTimeout.toMillis()
        return try {
            withTimeout(overallTimeoutMs) {
                supervisorScope {
                    providers.map { provider ->
                        async {
                            executeProvider(provider = provider, request = request)
                        }
                    }.map { deferred -> deferred.await() }
                }
            }
        } catch (_: TimeoutCancellationException) {
            providers.map { provider ->
                disabledResult(
                    provider = provider,
                    requestType = request.requestType,
                    status = ProviderStatus.TIMEOUT,
                    diagnostics = "Global provider execution timeout reached.",
                )
            }
        }
    }

    private suspend fun executeProvider(
        provider: ExecutableFoodCatalogProvider,
        request: ProviderExecutionRequest,
    ): ProviderResult {
        val startedAtMs = clock.millis()
        val capability = request.requestType.capability

        if (!provider.metadata.enabled) {
            return disabledResult(
                provider = provider,
                requestType = request.requestType,
                status = ProviderStatus.DISABLED_BY_SETTINGS,
                diagnostics = provider.metadata.statusReason,
            )
        }

        if (!provider.supports(capability)) {
            return disabledResult(
                provider = provider,
                requestType = request.requestType,
                status = ProviderStatus.UNSUPPORTED_CAPABILITY,
                diagnostics = "Provider does not support $capability.",
            )
        }

        val providerImpl = provider.provider
        if (providerImpl == null) {
            return disabledResult(
                provider = provider,
                requestType = request.requestType,
                status = ProviderStatus.MISCONFIGURED,
                diagnostics = "Provider implementation is missing.",
            )
        }

        val token = request.token
        if (token == null) {
            return disabledResult(
                provider = provider,
                requestType = request.requestType,
                status = ProviderStatus.GUARD_REJECTED,
                diagnostics = "Missing user-initiated token.",
            )
        }

        return try {
            val items = withTimeout(policy.perProviderTimeout.toMillis()) {
                when (request.requestType) {
                    ProviderRequestType.TEXT_SEARCH -> {
                        providerImpl.search(
                            query = request.query.orEmpty(),
                            token = token,
                        )
                    }

                    ProviderRequestType.BARCODE_LOOKUP -> {
                        providerImpl.lookupBarcode(
                            barcode = request.barcode.orEmpty(),
                            token = token,
                        )?.let { listOf(it) }.orEmpty()
                    }
                }
            }

            val stableItems = items.sortedWith(
                compareBy<RemoteFoodCandidate>(
                    { buildProviderDedupeKey(it) },
                    { "${it.source}:${it.sourceId}" },
                ),
            )
            ProviderResult(
                providerId = provider.metadata.key,
                capability = capability,
                status = if (stableItems.isEmpty()) ProviderStatus.EMPTY else ProviderStatus.AVAILABLE,
                items = stableItems,
                elapsedMs = elapsedSince(startedAtMs),
            )
        } catch (_: TimeoutCancellationException) {
            ProviderResult(
                providerId = provider.metadata.key,
                capability = capability,
                status = ProviderStatus.TIMEOUT,
                items = emptyList(),
                elapsedMs = elapsedSince(startedAtMs),
                diagnostics = "Provider execution timed out.",
            )
        } catch (_: CancellationException) {
            ProviderResult(
                providerId = provider.metadata.key,
                capability = capability,
                status = ProviderStatus.TIMEOUT,
                items = emptyList(),
                elapsedMs = elapsedSince(startedAtMs),
                diagnostics = "Provider execution cancelled.",
            )
        } catch (_: Exception) {
            ProviderResult(
                providerId = provider.metadata.key,
                capability = capability,
                status = ProviderStatus.ERROR,
                items = emptyList(),
                elapsedMs = elapsedSince(startedAtMs),
                diagnostics = "Provider execution failed.",
            )
        }
    }

    private fun mergeCandidates(providerResults: List<ProviderResult>): List<ProviderMergedCandidate> {
        val merged = mutableListOf<ProviderMergedCandidate>()
        val seenKeys = LinkedHashSet<String>()
        providerResults.forEach { result ->
            if (result.status != ProviderStatus.AVAILABLE && result.status != ProviderStatus.EMPTY) {
                return@forEach
            }
            result.items.forEach { candidate ->
                val dedupeKey = buildProviderDedupeKey(candidate)
                if (seenKeys.add(dedupeKey)) {
                    merged += ProviderMergedCandidate(
                        providerId = result.providerId,
                        candidate = candidate,
                        dedupeKey = dedupeKey,
                    )
                }
            }
        }
        return merged
    }

    private fun disabledResult(
        provider: ExecutableFoodCatalogProvider,
        requestType: ProviderRequestType,
        status: ProviderStatus,
        diagnostics: String,
    ): ProviderResult {
        return ProviderResult(
            providerId = provider.metadata.key,
            capability = requestType.capability,
            status = status,
            items = emptyList(),
            elapsedMs = 0L,
            diagnostics = diagnostics,
        )
    }

    private fun elapsedSince(startedAtMs: Long): Long {
        return (clock.millis() - startedAtMs).coerceAtLeast(0L)
    }
}

data class ExecutableFoodCatalogProvider(
    val metadata: FoodCatalogProviderDescriptor,
    val provider: FoodCatalogProvider?,
) {
    fun supports(capability: ProviderCapability): Boolean {
        return when (capability) {
            ProviderCapability.TEXT_SEARCH -> metadata.supportsTextSearch
            ProviderCapability.BARCODE_LOOKUP -> metadata.supportsBarcode
        }
    }
}
