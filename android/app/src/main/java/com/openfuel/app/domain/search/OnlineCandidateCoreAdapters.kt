package com.openfuel.app.domain.search

import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.domain.model.RemoteFoodSource
import com.openfuel.sharedcore.model.CoreRemoteFoodCandidate
import com.openfuel.sharedcore.online.CoreCandidateCompleteness
import com.openfuel.sharedcore.online.CoreCandidateSelectionReason
import com.openfuel.sharedcore.online.CoreServingReviewStatus

fun RemoteFoodCandidate.toCoreRemoteFoodCandidate(): CoreRemoteFoodCandidate {
    return CoreRemoteFoodCandidate(
        source = source.name,
        sourceId = sourceId,
        providerKey = providerKey,
        barcode = barcode,
        name = name,
        brand = brand,
        caloriesKcalPer100g = caloriesKcalPer100g,
        proteinGPer100g = proteinGPer100g,
        carbsGPer100g = carbsGPer100g,
        fatGPer100g = fatGPer100g,
        servingSize = servingSize,
    )
}

fun CoreRemoteFoodCandidate.toRemoteFoodCandidate(): RemoteFoodCandidate {
    return RemoteFoodCandidate(
        source = source.toRemoteFoodSource(),
        sourceId = sourceId,
        providerKey = providerKey,
        barcode = barcode,
        name = name,
        brand = brand,
        caloriesKcalPer100g = caloriesKcalPer100g,
        proteinGPer100g = proteinGPer100g,
        carbsGPer100g = carbsGPer100g,
        fatGPer100g = fatGPer100g,
        servingSize = servingSize,
    )
}

fun CoreCandidateSelectionReason.toOnlineCandidateSelectionReason(): OnlineCandidateSelectionReason {
    return when (this) {
        CoreCandidateSelectionReason.SINGLE_SOURCE_RESULT -> OnlineCandidateSelectionReason.SINGLE_SOURCE_RESULT
        CoreCandidateSelectionReason.BARCODE_MATCH -> OnlineCandidateSelectionReason.BARCODE_MATCH
        CoreCandidateSelectionReason.BEST_MATCH_ACROSS_SOURCES -> OnlineCandidateSelectionReason.BEST_MATCH_ACROSS_SOURCES
        CoreCandidateSelectionReason.MOST_COMPLETE_NUTRITION -> OnlineCandidateSelectionReason.MOST_COMPLETE_NUTRITION
        CoreCandidateSelectionReason.PREFERRED_SOURCE -> OnlineCandidateSelectionReason.PREFERRED_SOURCE
        CoreCandidateSelectionReason.DETERMINISTIC_TIE_BREAK -> OnlineCandidateSelectionReason.DETERMINISTIC_TIE_BREAK
    }
}

fun CoreCandidateCompleteness.toOnlineCandidateCompleteness(): OnlineCandidateCompleteness {
    return when (this) {
        CoreCandidateCompleteness.COMPLETE -> OnlineCandidateCompleteness.COMPLETE
        CoreCandidateCompleteness.PARTIAL -> OnlineCandidateCompleteness.PARTIAL
        CoreCandidateCompleteness.LIMITED -> OnlineCandidateCompleteness.LIMITED
    }
}

fun CoreServingReviewStatus.toOnlineServingReviewStatus(): OnlineServingReviewStatus {
    return when (this) {
        CoreServingReviewStatus.OK -> OnlineServingReviewStatus.OK
        CoreServingReviewStatus.NEEDS_REVIEW -> OnlineServingReviewStatus.NEEDS_REVIEW
    }
}

private fun String.toRemoteFoodSource(): RemoteFoodSource {
    return RemoteFoodSource.entries.firstOrNull { source -> source.name == this }
        ?: RemoteFoodSource.STATIC_SAMPLE
}
