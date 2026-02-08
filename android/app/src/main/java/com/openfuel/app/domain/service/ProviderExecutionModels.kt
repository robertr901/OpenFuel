package com.openfuel.app.domain.service

import com.openfuel.app.data.remote.UserInitiatedNetworkToken
import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.domain.search.SearchSourceFilter
import java.time.Duration
import java.time.Instant
import java.util.Locale

enum class ProviderCapability {
    TEXT_SEARCH,
    BARCODE_LOOKUP,
}

enum class ProviderRequestType(
    val capability: ProviderCapability,
) {
    TEXT_SEARCH(ProviderCapability.TEXT_SEARCH),
    BARCODE_LOOKUP(ProviderCapability.BARCODE_LOOKUP),
}

enum class ProviderStatus {
    AVAILABLE,
    EMPTY,
    DISABLED_BY_SOURCE_FILTER,
    DISABLED_BY_SETTINGS,
    UNSUPPORTED_CAPABILITY,
    MISCONFIGURED,
    RATE_LIMITED,
    TIMEOUT,
    GUARD_REJECTED,
    ERROR,
}

enum class ProviderRefreshPolicy {
    CACHE_PREFERRED,
    FORCE_REFRESH,
}

data class ProviderExecutionPolicy(
    val overallTimeout: Duration = Duration.ofSeconds(6),
    val perProviderTimeout: Duration = Duration.ofSeconds(3),
    val cacheTtl: Duration = Duration.ofHours(24),
) {
    init {
        require(!overallTimeout.isNegative && !overallTimeout.isZero) {
            "overallTimeout must be greater than zero."
        }
        require(!perProviderTimeout.isNegative && !perProviderTimeout.isZero) {
            "perProviderTimeout must be greater than zero."
        }
        require(overallTimeout >= perProviderTimeout) {
            "overallTimeout must be greater than or equal to perProviderTimeout."
        }
        require(!cacheTtl.isNegative && !cacheTtl.isZero) {
            "cacheTtl must be greater than zero."
        }
    }
}

data class ProviderExecutionRequest(
    val requestType: ProviderRequestType,
    val sourceFilter: SearchSourceFilter,
    val onlineLookupEnabled: Boolean = true,
    val query: String? = null,
    val barcode: String? = null,
    val token: UserInitiatedNetworkToken? = null,
    val refreshPolicy: ProviderRefreshPolicy = ProviderRefreshPolicy.CACHE_PREFERRED,
) {
    init {
        when (requestType) {
            ProviderRequestType.TEXT_SEARCH -> {
                require(!query.isNullOrBlank()) {
                    "query is required for text-search requests."
                }
            }

            ProviderRequestType.BARCODE_LOOKUP -> {
                require(!barcode.isNullOrBlank()) {
                    "barcode is required for barcode lookup requests."
                }
            }
        }
    }
}

data class ProviderResult(
    val providerId: String,
    val capability: ProviderCapability,
    val status: ProviderStatus,
    val items: List<RemoteFoodCandidate>,
    val elapsedMs: Long,
    val diagnostics: String? = null,
    val fromCache: Boolean = false,
)

data class ProviderCacheStats(
    val hitCount: Int = 0,
    val missCount: Int = 0,
)

data class ProviderExecutionReport(
    val requestType: ProviderRequestType,
    val sourceFilter: SearchSourceFilter,
    val mergedCandidates: List<ProviderMergedCandidate>,
    val providerResults: List<ProviderResult>,
    val overallElapsedMs: Long,
    val cacheStats: ProviderCacheStats = ProviderCacheStats(),
    val executedAt: Instant = Instant.now(),
)

data class ProviderMergedCandidate(
    val providerId: String,
    val candidate: RemoteFoodCandidate,
    val dedupeKey: String,
)

interface ProviderExecutor {
    suspend fun execute(request: ProviderExecutionRequest): ProviderExecutionReport
}

fun buildProviderDedupeKey(candidate: RemoteFoodCandidate): String {
    val barcode = normalizeProviderBarcode(candidate.barcode)
    if (barcode != null) {
        return "barcode:$barcode"
    }
    val name = normalizeProviderText(candidate.name)
    val brand = normalizeProviderText(candidate.brand.orEmpty())
    val servingSize = normalizeProviderText(candidate.servingSize.orEmpty())
    return "text:$name|$brand|$servingSize"
}

fun buildProviderCacheKey(
    providerId: String,
    requestType: ProviderRequestType,
    rawInput: String,
): String {
    val normalizedProviderId = normalizeProviderText(providerId)
    val normalizedInput = when (requestType) {
        ProviderRequestType.TEXT_SEARCH -> normalizeProviderText(rawInput)
        ProviderRequestType.BARCODE_LOOKUP -> normalizeProviderBarcode(rawInput).orEmpty()
    }
    return "$normalizedProviderId|${requestType.name}|$normalizedInput"
}

fun normalizeProviderText(value: String): String {
    return value
        .trim()
        .lowercase(Locale.ROOT)
        .replace("\\s+".toRegex(), " ")
}

fun normalizeProviderBarcode(value: String?): String? {
    return value?.trim()?.takeIf { it.isNotEmpty() }
}
