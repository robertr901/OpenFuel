package com.openfuel.sharedcore.online

import com.openfuel.sharedcore.model.CoreRemoteFoodCandidate
import com.openfuel.sharedcore.normalization.buildProviderDedupeKey
import com.openfuel.sharedcore.normalization.normalizeProviderBarcode
import com.openfuel.sharedcore.normalization.normalizeProviderText

enum class CoreCandidateSelectionReason {
    SINGLE_SOURCE_RESULT,
    BARCODE_MATCH,
    BEST_MATCH_ACROSS_SOURCES,
    MOST_COMPLETE_NUTRITION,
    PREFERRED_SOURCE,
    DETERMINISTIC_TIE_BREAK,
}

data class CoreCandidateDecision(
    val selectedProviderId: String,
    val contributingProviderIds: List<String>,
    val reason: CoreCandidateSelectionReason,
)

data class CoreProviderCandidates(
    val providerId: String,
    val candidates: List<CoreRemoteFoodCandidate>,
)

data class CoreMergedCandidatesReport(
    val candidates: List<CoreRemoteFoodCandidate>,
    val candidateDecisions: Map<String, CoreCandidateDecision>,
)

fun mergeCandidates(
    providerCandidates: List<CoreProviderCandidates>,
    priorityByProviderId: Map<String, Int>,
): CoreMergedCandidatesReport {
    val buckets = LinkedHashMap<String, CandidateBucket>()
    val sortedProviders = providerCandidates.sortedWith(
        compareBy<CoreProviderCandidates> { provider ->
            priorityByProviderId[provider.providerId] ?: Int.MAX_VALUE
        }.thenBy { provider -> provider.providerId },
    )

    sortedProviders.forEach { provider ->
        provider.candidates.forEach { candidate ->
            val key = dedupeIdentityKey(candidate)
            val bucket = buckets[key.identity]
            if (bucket == null) {
                buckets[key.identity] = CandidateBucket(
                    key = key,
                    selectedProviderId = provider.providerId,
                    selectedCandidate = candidate,
                    contributingProviders = linkedSetOf(provider.providerId),
                    latestSelectionReason = null,
                )
            } else {
                bucket.contributingProviders += provider.providerId
                val selectionDecision = evaluateIncomingSelection(
                    existing = bucket.selectedCandidate,
                    existingProviderId = bucket.selectedProviderId,
                    incoming = candidate,
                    incomingProviderId = provider.providerId,
                    priorityByProviderId = priorityByProviderId,
                )
                bucket.latestSelectionReason = selectionDecision.reason
                if (selectionDecision.selectIncoming) {
                    bucket.selectedCandidate = candidate
                    bucket.selectedProviderId = provider.providerId
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

    val selectedCandidates = sortedBuckets.map { bucket ->
        bucket.selectedCandidate.copy(providerKey = bucket.selectedProviderId)
    }
    val decisions = sortedBuckets.associate { bucket ->
        val selectedCandidate = bucket.selectedCandidate.copy(providerKey = bucket.selectedProviderId)
        coreCandidateDecisionKey(selectedCandidate) to CoreCandidateDecision(
            selectedProviderId = bucket.selectedProviderId,
            contributingProviderIds = bucket.contributingProviders.toList(),
            reason = finalSelectionReason(bucket),
        )
    }
    return CoreMergedCandidatesReport(
        candidates = selectedCandidates,
        candidateDecisions = decisions,
    )
}

private fun finalSelectionReason(bucket: CandidateBucket): CoreCandidateSelectionReason {
    if (bucket.key.rank == 0) {
        return CoreCandidateSelectionReason.BARCODE_MATCH
    }
    if (bucket.contributingProviders.size == 1) {
        return CoreCandidateSelectionReason.SINGLE_SOURCE_RESULT
    }
    return bucket.latestSelectionReason ?: CoreCandidateSelectionReason.BEST_MATCH_ACROSS_SOURCES
}

private fun evaluateIncomingSelection(
    existing: CoreRemoteFoodCandidate,
    existingProviderId: String,
    incoming: CoreRemoteFoodCandidate,
    incomingProviderId: String,
    priorityByProviderId: Map<String, Int>,
): CandidateSelectionDecision {
    val incomingRichness = candidateRichnessScore(incoming)
    val existingRichness = candidateRichnessScore(existing)
    if (incomingRichness != existingRichness) {
        return CandidateSelectionDecision(
            selectIncoming = incomingRichness > existingRichness,
            reason = CoreCandidateSelectionReason.MOST_COMPLETE_NUTRITION,
        )
    }
    val incomingPriority = priorityByProviderId[incomingProviderId] ?: Int.MAX_VALUE
    val existingPriority = priorityByProviderId[existingProviderId] ?: Int.MAX_VALUE
    if (incomingPriority != existingPriority) {
        return CandidateSelectionDecision(
            selectIncoming = incomingPriority < existingPriority,
            reason = CoreCandidateSelectionReason.PREFERRED_SOURCE,
        )
    }
    val incomingIdentity = "${incoming.source}:${incoming.sourceId}"
    val existingIdentity = "${existing.source}:${existing.sourceId}"
    if (incomingIdentity != existingIdentity) {
        return CandidateSelectionDecision(
            selectIncoming = incomingIdentity < existingIdentity,
            reason = CoreCandidateSelectionReason.DETERMINISTIC_TIE_BREAK,
        )
    }
    return CandidateSelectionDecision(
        selectIncoming = incomingProviderId < existingProviderId,
        reason = CoreCandidateSelectionReason.DETERMINISTIC_TIE_BREAK,
    )
}

private fun candidateRichnessScore(candidate: CoreRemoteFoodCandidate): Int {
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

private fun dedupeIdentityKey(candidate: CoreRemoteFoodCandidate): CandidateIdentityKey {
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
    var selectedCandidate: CoreRemoteFoodCandidate,
    val contributingProviders: LinkedHashSet<String>,
    var latestSelectionReason: CoreCandidateSelectionReason?,
)

private data class CandidateSelectionDecision(
    val selectIncoming: Boolean,
    val reason: CoreCandidateSelectionReason,
)
