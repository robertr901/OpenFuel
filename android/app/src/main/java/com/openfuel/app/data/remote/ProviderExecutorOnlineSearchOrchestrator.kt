package com.openfuel.app.data.remote

import com.openfuel.app.domain.search.OnlineCandidateDecision
import com.openfuel.app.domain.search.OnlineCandidateSelectionReason
import com.openfuel.app.domain.search.OnlineProviderRun
import com.openfuel.app.domain.search.OnlineProviderRunStatus
import com.openfuel.app.domain.search.OnlineSearchOrchestrator
import com.openfuel.app.domain.search.OnlineSearchRequest
import com.openfuel.app.domain.search.OnlineSearchResult
import com.openfuel.app.domain.search.OnlineSearchSummary
import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.domain.search.SearchSourceFilter
import com.openfuel.app.domain.search.onlineCandidateDecisionKey
import com.openfuel.app.domain.service.FoodCatalogExecutionProvider
import com.openfuel.app.domain.service.FoodCatalogProviderRegistry
import com.openfuel.app.domain.service.ProviderExecutionRequest
import com.openfuel.app.domain.service.ProviderExecutor
import com.openfuel.app.domain.service.ProviderRequestType
import com.openfuel.app.domain.service.ProviderResult
import com.openfuel.app.domain.service.ProviderStatus
import com.openfuel.app.domain.service.buildProviderDedupeKey
import com.openfuel.app.domain.service.normalizeProviderBarcode
import com.openfuel.app.domain.service.normalizeProviderText

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
        val mergedCandidatesReport = mergeCandidates(
            providerResults = report.providerResults,
            priorityByProviderId = priorityByProviderId,
        )
        val candidates = mergedCandidatesReport.candidates
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
            candidateDecisions = mergedCandidatesReport.candidateDecisions,
        )
    }
}

private fun mergeCandidates(
    providerResults: List<ProviderResult>,
    priorityByProviderId: Map<String, Int>,
): MergedCandidatesReport {
    val buckets = LinkedHashMap<String, CandidateBucket>()
    val sortedResults = providerResults.sortedWith(
        compareBy<ProviderResult> { result ->
            priorityByProviderId[result.providerId] ?: Int.MAX_VALUE
        }.thenBy { result -> result.providerId },
    )

    sortedResults.forEach { result ->
        if (result.status != ProviderStatus.AVAILABLE && result.status != ProviderStatus.EMPTY) {
            return@forEach
        }
        result.items.forEach { candidate ->
            val key = dedupeIdentityKey(candidate)
            val bucket = buckets[key.identity]
            if (bucket == null) {
                buckets[key.identity] = CandidateBucket(
                    key = key,
                    selectedProviderId = result.providerId,
                    selectedCandidate = candidate,
                    contributingProviders = linkedSetOf(result.providerId),
                    latestSelectionReason = null,
                )
            } else {
                bucket.contributingProviders += result.providerId
                val selectionDecision = evaluateIncomingSelection(
                    existing = bucket.selectedCandidate,
                    existingProviderId = bucket.selectedProviderId,
                    incoming = candidate,
                    incomingProviderId = result.providerId,
                    priorityByProviderId = priorityByProviderId,
                )
                bucket.latestSelectionReason = selectionDecision.reason
                if (selectionDecision.selectIncoming) {
                    bucket.selectedCandidate = candidate
                    bucket.selectedProviderId = result.providerId
                }
            }
        }
    }

    val sortedBuckets = buckets.values
        .sortedWith(
            compareBy<CandidateBucket> { bucket -> bucket.key.rank }
                .thenBy { bucket -> bucket.key.identity }
                .thenBy { bucket -> priorityByProviderId[bucket.selectedProviderId] ?: Int.MAX_VALUE }
                .thenBy { bucket -> bucket.selectedProviderId }
                .thenBy { bucket -> "${bucket.selectedCandidate.source}:${bucket.selectedCandidate.sourceId}" },
        )
    val candidates = sortedBuckets.map { bucket ->
        bucket.selectedCandidate.copy(providerKey = bucket.selectedProviderId)
    }
    val decisions = sortedBuckets.associate { bucket ->
        val selectedCandidate = bucket.selectedCandidate.copy(providerKey = bucket.selectedProviderId)
        onlineCandidateDecisionKey(selectedCandidate) to OnlineCandidateDecision(
            selectedProviderId = bucket.selectedProviderId,
            contributingProviderIds = bucket.contributingProviders.toList(),
            reason = finalSelectionReason(bucket),
        )
    }
    return MergedCandidatesReport(
        candidates = candidates,
        candidateDecisions = decisions,
    )
}

private fun finalSelectionReason(bucket: CandidateBucket): OnlineCandidateSelectionReason {
    if (bucket.key.rank == 0) {
        return OnlineCandidateSelectionReason.BARCODE_MATCH
    }
    if (bucket.contributingProviders.size == 1) {
        return OnlineCandidateSelectionReason.SINGLE_SOURCE_RESULT
    }
    return bucket.latestSelectionReason ?: OnlineCandidateSelectionReason.BEST_MATCH_ACROSS_SOURCES
}

private fun evaluateIncomingSelection(
    existing: RemoteFoodCandidate,
    existingProviderId: String,
    incoming: RemoteFoodCandidate,
    incomingProviderId: String,
    priorityByProviderId: Map<String, Int>,
): CandidateSelectionDecision {
    val incomingRichness = candidateRichnessScore(incoming)
    val existingRichness = candidateRichnessScore(existing)
    if (incomingRichness != existingRichness) {
        return CandidateSelectionDecision(
            selectIncoming = incomingRichness > existingRichness,
            reason = OnlineCandidateSelectionReason.MOST_COMPLETE_NUTRITION,
        )
    }
    val incomingPriority = priorityByProviderId[incomingProviderId] ?: Int.MAX_VALUE
    val existingPriority = priorityByProviderId[existingProviderId] ?: Int.MAX_VALUE
    if (incomingPriority != existingPriority) {
        return CandidateSelectionDecision(
            selectIncoming = incomingPriority < existingPriority,
            reason = OnlineCandidateSelectionReason.PREFERRED_SOURCE,
        )
    }
    val incomingIdentity = "${incoming.source}:${incoming.sourceId}"
    val existingIdentity = "${existing.source}:${existing.sourceId}"
    if (incomingIdentity != existingIdentity) {
        return CandidateSelectionDecision(
            selectIncoming = incomingIdentity < existingIdentity,
            reason = OnlineCandidateSelectionReason.DETERMINISTIC_TIE_BREAK,
        )
    }
    return CandidateSelectionDecision(
        selectIncoming = incomingProviderId < existingProviderId,
        reason = OnlineCandidateSelectionReason.DETERMINISTIC_TIE_BREAK,
    )
}

private fun candidateRichnessScore(candidate: RemoteFoodCandidate): Int {
    var score = 0
    if (candidate.caloriesKcalPer100g != null) score += 2
    if (candidate.proteinGPer100g != null) score += 1
    if (candidate.carbsGPer100g != null) score += 1
    if (candidate.fatGPer100g != null) score += 1
    if (!candidate.brand.isNullOrBlank()) score += 1
    if (!candidate.servingSize.isNullOrBlank()) score += 1
    if (!candidate.barcode.isNullOrBlank()) score += 1
    return score
}

private fun dedupeIdentityKey(candidate: RemoteFoodCandidate): CandidateIdentityKey {
    val barcode = normalizeProviderBarcode(candidate.barcode)
    if (!barcode.isNullOrBlank()) {
        return CandidateIdentityKey(
            rank = 0,
            identity = "barcode:$barcode",
        )
    }
    val normalizedName = normalizeProviderText(candidate.name)
    if (normalizedName.isNotBlank()) {
        val normalizedBrand = normalizeProviderText(candidate.brand.orEmpty())
        val normalizedServing = normalizeProviderText(candidate.servingSize.orEmpty())
        if (normalizedBrand.isNotBlank() && normalizedServing.isNotBlank()) {
            return CandidateIdentityKey(
                rank = 1,
                identity = "name:$normalizedName|brand:$normalizedBrand|serving:$normalizedServing",
            )
        }
        if (normalizedBrand.isNotBlank()) {
            return CandidateIdentityKey(
                rank = 2,
                identity = "name:$normalizedName|brand:$normalizedBrand",
            )
        }
        if (normalizedServing.isNotBlank()) {
            return CandidateIdentityKey(
                rank = 2,
                identity = "name:$normalizedName|serving:$normalizedServing",
            )
        }
        return CandidateIdentityKey(
            rank = 3,
            identity = "name:$normalizedName",
        )
    }
    return CandidateIdentityKey(
        rank = 4,
        identity = "fuzzy:${buildProviderDedupeKey(candidate)}",
    )
}

private data class CandidateIdentityKey(
    val rank: Int,
    val identity: String,
)

private data class CandidateBucket(
    val key: CandidateIdentityKey,
    var selectedProviderId: String,
    var selectedCandidate: RemoteFoodCandidate,
    val contributingProviders: LinkedHashSet<String>,
    var latestSelectionReason: OnlineCandidateSelectionReason?,
)

private data class CandidateSelectionDecision(
    val selectIncoming: Boolean,
    val reason: OnlineCandidateSelectionReason,
)

private data class MergedCandidatesReport(
    val candidates: List<RemoteFoodCandidate>,
    val candidateDecisions: Map<String, OnlineCandidateDecision>,
)

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
