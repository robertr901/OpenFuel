package com.openfuel.app.domain.search

import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.sharedcore.online.coreCandidateDecisionKey
import com.openfuel.sharedcore.online.deriveCoreCandidateTrustSignals
import com.openfuel.sharedcore.online.deriveCoreCompleteness
import com.openfuel.sharedcore.online.deriveCoreProvenanceLabel
import com.openfuel.sharedcore.online.deriveCoreServingReviewStatus

enum class OnlineCandidateCompleteness {
    COMPLETE,
    PARTIAL,
    LIMITED,
}

enum class OnlineServingReviewStatus {
    OK,
    NEEDS_REVIEW,
}

data class OnlineCandidateTrustSignals(
    val decisionKey: String,
    val provenanceLabel: String,
    val completeness: OnlineCandidateCompleteness,
    val servingReviewStatus: OnlineServingReviewStatus,
)

fun onlineCandidateDecisionKey(candidate: RemoteFoodCandidate): String {
    return coreCandidateDecisionKey(candidate.toCoreRemoteFoodCandidate())
}

fun deriveOnlineCandidateTrustSignals(
    candidate: RemoteFoodCandidate,
): OnlineCandidateTrustSignals {
    val coreSignals = deriveCoreCandidateTrustSignals(candidate.toCoreRemoteFoodCandidate())
    return OnlineCandidateTrustSignals(
        decisionKey = coreSignals.decisionKey,
        provenanceLabel = coreSignals.provenanceLabel,
        completeness = coreSignals.completeness.toOnlineCandidateCompleteness(),
        servingReviewStatus = coreSignals.servingReviewStatus.toOnlineServingReviewStatus(),
    )
}

fun deriveProvenanceLabel(candidate: RemoteFoodCandidate): String {
    return deriveCoreProvenanceLabel(candidate.toCoreRemoteFoodCandidate())
}

fun deriveCompleteness(candidate: RemoteFoodCandidate): OnlineCandidateCompleteness {
    return deriveCoreCompleteness(candidate.toCoreRemoteFoodCandidate()).toOnlineCandidateCompleteness()
}

fun deriveServingReviewStatus(servingSize: String?): OnlineServingReviewStatus {
    return deriveCoreServingReviewStatus(servingSize).toOnlineServingReviewStatus()
}
