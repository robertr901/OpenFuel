package com.openfuel.app.domain.search

import com.openfuel.app.data.remote.UserInitiatedNetworkToken
import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.domain.service.FoodCatalogExecutionProvider
import com.openfuel.app.domain.service.ProviderRefreshPolicy
import com.openfuel.app.domain.service.ProviderResult

enum class OnlineProviderRunStatus {
    SUCCESS,
    EMPTY,
    FAILED,
    SKIPPED_MISSING_CONFIG,
    SKIPPED_DISABLED,
}

enum class OnlineCandidateSelectionReason {
    SINGLE_SOURCE_RESULT,
    BARCODE_MATCH,
    BEST_MATCH_ACROSS_SOURCES,
    MOST_COMPLETE_NUTRITION,
    PREFERRED_SOURCE,
    DETERMINISTIC_TIE_BREAK,
}

data class OnlineCandidateDecision(
    val selectedProviderId: String,
    val contributingProviderIds: List<String>,
    val reason: OnlineCandidateSelectionReason,
)

data class OnlineProviderRun(
    val providerId: String,
    val providerDisplayName: String,
    val status: OnlineProviderRunStatus,
    val message: String?,
    val durationMs: Long,
    val candidateCount: Int,
)

data class OnlineSearchSummary(
    val totalCandidates: Int,
    val successfulProviders: Int,
    val failedProviders: Int,
    val skippedProviders: Int,
)

data class OnlineSearchResult(
    val providerRuns: List<OnlineProviderRun>,
    val candidates: List<RemoteFoodCandidate>,
    val summary: OnlineSearchSummary,
    val overallDurationMs: Long,
    val providerResults: List<ProviderResult>,
    val candidateDecisions: Map<String, OnlineCandidateDecision> = emptyMap(),
)

data class OnlineSearchRequest(
    val query: String,
    val token: UserInitiatedNetworkToken,
    val onlineLookupEnabled: Boolean,
    val refreshPolicy: ProviderRefreshPolicy,
    val providers: List<FoodCatalogExecutionProvider> = emptyList(),
)

interface OnlineSearchOrchestrator {
    suspend fun search(request: OnlineSearchRequest): OnlineSearchResult
}
