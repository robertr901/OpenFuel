package com.openfuel.app.data.remote

import com.openfuel.app.domain.search.OnlineCandidateDecision
import com.openfuel.app.domain.search.OnlineProviderRun
import com.openfuel.app.domain.search.OnlineProviderRunStatus
import com.openfuel.app.domain.search.OnlineSearchOrchestrator
import com.openfuel.app.domain.search.OnlineSearchRequest
import com.openfuel.app.domain.search.OnlineSearchResult
import com.openfuel.app.domain.search.OnlineSearchSummary
import com.openfuel.app.domain.search.toCoreRemoteFoodCandidate
import com.openfuel.app.domain.search.toOnlineCandidateSelectionReason
import com.openfuel.app.domain.search.toRemoteFoodCandidate
import com.openfuel.app.domain.search.SearchSourceFilter
import com.openfuel.app.domain.service.FoodCatalogExecutionProvider
import com.openfuel.app.domain.service.FoodCatalogProviderRegistry
import com.openfuel.app.domain.service.ProviderExecutionRequest
import com.openfuel.app.domain.service.ProviderExecutor
import com.openfuel.app.domain.service.ProviderRefreshPolicy
import com.openfuel.app.domain.service.ProviderRequestType
import com.openfuel.app.domain.service.ProviderResult
import com.openfuel.app.domain.service.ProviderStatus
import com.openfuel.sharedcore.online.CoreProviderCandidates
import com.openfuel.sharedcore.online.mergeCandidates as mergeCoreCandidates

class ProviderExecutorOnlineSearchOrchestrator(
    private val providerExecutor: ProviderExecutor,
    private val providerRegistry: FoodCatalogProviderRegistry,
) : OnlineSearchOrchestrator {
    override suspend fun search(request: OnlineSearchRequest): OnlineSearchResult {
        val executionProviders = request.providers
            .takeIf { providers -> providers.isNotEmpty() }
            ?: providerRegistry.providersFor(
                requestType = ProviderRequestType.TEXT_SEARCH,
                onlineLookupEnabled = request.onlineLookupEnabled,
            )
        val sortedProviders = executionProviders.sortedWith(
            compareBy<FoodCatalogExecutionProvider> { it.descriptor.priority }
                .thenBy { it.descriptor.key },
        )

        val report = providerExecutor.execute(
            request = ProviderExecutionRequest(
                requestType = ProviderRequestType.TEXT_SEARCH,
                sourceFilter = SearchSourceFilter.ONLINE_ONLY,
                onlineLookupEnabled = request.onlineLookupEnabled,
                query = request.query,
                token = request.token,
                refreshPolicy = request.refreshPolicy,
            ),
        )

        val providerResultsById = report.providerResults.associateBy { result -> result.providerId }
        val providerRuns = if (sortedProviders.isEmpty()) {
            report.providerResults
                .sortedBy { result -> result.providerId }
                .map { result ->
                    result.toOnlineProviderRun(
                        providerId = result.providerId,
                        providerDisplayName = result.providerId,
                        fallbackMessage = result.diagnostics ?: "Configured.",
                    )
                }
        } else {
            sortedProviders.map { provider ->
                val descriptor = provider.descriptor
                val result = providerResultsById[descriptor.key]
                result.toOnlineProviderRun(
                    providerId = descriptor.key,
                    providerDisplayName = descriptor.displayName,
                    fallbackMessage = descriptor.statusReason,
                )
            }
        }.filterNot { run ->
            run.providerId.endsWith("_stub")
        }

        val priorityByProviderId = sortedProviders
            .mapIndexed { index, provider -> provider.descriptor.key to index }
            .toMap()
        val mergedCandidatesReport = mergeCoreCandidates(
            providerCandidates = report.providerResults
                .sortedWith(
                    compareBy<ProviderResult> { result ->
                        priorityByProviderId[result.providerId] ?: Int.MAX_VALUE
                    }.thenBy { result -> result.providerId },
                )
                .filter { result ->
                    result.status == ProviderStatus.AVAILABLE || result.status == ProviderStatus.EMPTY
                }
                .map { result ->
                    CoreProviderCandidates(
                        providerId = result.providerId,
                        candidates = result.items.map { candidate ->
                            candidate.toCoreRemoteFoodCandidate()
                        },
                    )
                },
            priorityByProviderId = priorityByProviderId,
        )
        val candidates = mergedCandidatesReport.candidates.map { candidate ->
            candidate.toRemoteFoodCandidate()
        }
        val candidateDecisions = mergedCandidatesReport.candidateDecisions.mapValues { (_, decision) ->
            OnlineCandidateDecision(
                selectedProviderId = decision.selectedProviderId,
                contributingProviderIds = decision.contributingProviderIds,
                reason = decision.reason.toOnlineCandidateSelectionReason(),
            )
        }

        val summary = OnlineSearchSummary(
            totalCandidates = candidates.size,
            successfulProviders = providerRuns.count { run ->
                run.status == OnlineProviderRunStatus.SUCCESS ||
                    run.status == OnlineProviderRunStatus.EMPTY
            },
            failedProviders = providerRuns.count { run ->
                run.status == OnlineProviderRunStatus.FAILED
            },
            skippedProviders = providerRuns.count { run ->
                run.status == OnlineProviderRunStatus.SKIPPED_DISABLED ||
                    run.status == OnlineProviderRunStatus.SKIPPED_MISSING_CONFIG
            },
        )

        return OnlineSearchResult(
            providerRuns = providerRuns,
            candidates = candidates,
            summary = summary,
            overallDurationMs = report.overallElapsedMs,
            providerResults = report.providerResults,
            candidateDecisions = candidateDecisions,
        )
    }
}

private fun ProviderResult?.toOnlineProviderRun(
    providerId: String,
    providerDisplayName: String,
    fallbackMessage: String,
): OnlineProviderRun {
    if (this == null) {
        return OnlineProviderRun(
            providerId = providerId,
            providerDisplayName = providerDisplayName,
            status = OnlineProviderRunStatus.SKIPPED_DISABLED,
            message = fallbackMessage,
            durationMs = 0L,
            candidateCount = 0,
        )
    }
    val status = toOnlineRunStatus(diagnostics = diagnostics)
    val message = toOnlineRunMessage(status = status)
    return OnlineProviderRun(
        providerId = providerId,
        providerDisplayName = providerDisplayName,
        status = status,
        message = message,
        durationMs = elapsedMs,
        candidateCount = items.size,
    )
}

private fun ProviderResult.toOnlineRunStatus(
    diagnostics: String?,
): OnlineProviderRunStatus {
    return when (status) {
        ProviderStatus.AVAILABLE -> OnlineProviderRunStatus.SUCCESS
        ProviderStatus.EMPTY -> OnlineProviderRunStatus.EMPTY
        ProviderStatus.DISABLED_BY_SETTINGS -> {
            if (diagnostics.containsMissingConfigHint()) {
                OnlineProviderRunStatus.SKIPPED_MISSING_CONFIG
            } else {
                OnlineProviderRunStatus.SKIPPED_DISABLED
            }
        }
        ProviderStatus.DISABLED_BY_SOURCE_FILTER,
        ProviderStatus.UNSUPPORTED_CAPABILITY,
        ProviderStatus.MISCONFIGURED,
        -> OnlineProviderRunStatus.SKIPPED_DISABLED
        ProviderStatus.RATE_LIMITED,
        ProviderStatus.NETWORK_UNAVAILABLE,
        ProviderStatus.HTTP_ERROR,
        ProviderStatus.PARSING_ERROR,
        ProviderStatus.TIMEOUT,
        ProviderStatus.GUARD_REJECTED,
        ProviderStatus.ERROR,
        -> OnlineProviderRunStatus.FAILED
    }
}

private fun ProviderResult.toOnlineRunMessage(
    status: OnlineProviderRunStatus,
): String? {
    return when (status) {
        OnlineProviderRunStatus.SUCCESS,
        OnlineProviderRunStatus.EMPTY,
        -> null
        OnlineProviderRunStatus.SKIPPED_MISSING_CONFIG -> diagnostics ?: "Needs setup."
        OnlineProviderRunStatus.SKIPPED_DISABLED -> diagnostics ?: "Disabled."
        OnlineProviderRunStatus.FAILED -> when (this.status) {
            ProviderStatus.NETWORK_UNAVAILABLE -> "No connection."
            ProviderStatus.HTTP_ERROR -> "Service error."
            ProviderStatus.PARSING_ERROR -> "Service error."
            ProviderStatus.RATE_LIMITED -> "Rate limited."
            ProviderStatus.TIMEOUT -> "Timed out (check connection)."
            ProviderStatus.GUARD_REJECTED -> "Network guard rejected request."
            ProviderStatus.ERROR -> "Provider failed."
            else -> diagnostics ?: "Provider failed."
        }
    }
}

private fun String?.containsMissingConfigHint(): Boolean {
    if (this.isNullOrBlank()) {
        return false
    }
    val normalized = lowercase()
    return normalized.contains("api key missing") ||
        normalized.contains("needs setup") ||
        normalized.contains("not configured")
}
