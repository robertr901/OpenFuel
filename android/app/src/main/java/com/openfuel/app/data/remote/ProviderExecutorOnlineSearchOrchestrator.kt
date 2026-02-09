package com.openfuel.app.data.remote

import com.openfuel.app.domain.search.OnlineProviderRun
import com.openfuel.app.domain.search.OnlineProviderRunStatus
import com.openfuel.app.domain.search.OnlineSearchOrchestrator
import com.openfuel.app.domain.search.OnlineSearchRequest
import com.openfuel.app.domain.search.OnlineSearchResult
import com.openfuel.app.domain.search.OnlineSearchSummary
import com.openfuel.app.domain.search.SearchSourceFilter
import com.openfuel.app.domain.service.FoodCatalogExecutionProvider
import com.openfuel.app.domain.service.FoodCatalogProviderRegistry
import com.openfuel.app.domain.service.ProviderExecutionRequest
import com.openfuel.app.domain.service.ProviderExecutor
import com.openfuel.app.domain.service.ProviderRequestType
import com.openfuel.app.domain.service.ProviderResult
import com.openfuel.app.domain.service.ProviderStatus

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
        val providerRuns = sortedProviders.map { provider ->
            val descriptor = provider.descriptor
            val result = providerResultsById[descriptor.key]
            result.toOnlineProviderRun(
                providerId = descriptor.key,
                providerDisplayName = descriptor.displayName,
                fallbackMessage = descriptor.statusReason,
            )
        }.filterNot { run ->
            run.providerId.endsWith("_stub")
        }

        val candidates = report.mergedCandidates.map { merged ->
            merged.candidate.copy(providerKey = merged.providerId)
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
            ProviderStatus.NETWORK_UNAVAILABLE -> "Network unavailable."
            ProviderStatus.HTTP_ERROR -> "Service error."
            ProviderStatus.PARSING_ERROR -> "Response parsing failed."
            ProviderStatus.RATE_LIMITED -> "Rate limited."
            ProviderStatus.TIMEOUT -> "Request timed out."
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
