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
