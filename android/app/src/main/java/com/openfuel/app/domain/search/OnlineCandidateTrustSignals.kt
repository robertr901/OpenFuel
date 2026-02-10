package com.openfuel.app.domain.search

import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.domain.model.RemoteFoodSource
import java.util.Locale

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

private val knownServingUnits = setOf(
    "mg",
    "g",
    "kg",
    "ml",
    "l",
    "oz",
    "lb",
    "cup",
    "cups",
    "tbsp",
    "tsp",
    "serving",
    "servings",
    "piece",
    "pieces",
    "can",
    "cans",
    "bottle",
    "bottles",
    "bar",
    "bars",
    "biscuit",
    "biscuits",
    "packet",
    "packets",
)

fun onlineCandidateDecisionKey(candidate: RemoteFoodCandidate): String {
    return "${candidate.source}:${candidate.sourceId}"
}

fun deriveOnlineCandidateTrustSignals(
    candidate: RemoteFoodCandidate,
): OnlineCandidateTrustSignals {
    return OnlineCandidateTrustSignals(
        decisionKey = onlineCandidateDecisionKey(candidate),
        provenanceLabel = deriveProvenanceLabel(candidate),
        completeness = deriveCompleteness(candidate),
        servingReviewStatus = deriveServingReviewStatus(candidate.servingSize),
    )
}

fun deriveProvenanceLabel(candidate: RemoteFoodCandidate): String {
    val providerKey = candidate.providerKey.orEmpty()
    return when {
        providerKey.equals("open_food_facts", ignoreCase = true) -> "OFF"
        providerKey.equals("usda_fdc", ignoreCase = true) -> "USDA"
        providerKey.equals("nutritionix", ignoreCase = true) -> "Nutritionix"
        providerKey.equals("static_sample", ignoreCase = true) -> "Sample"
        providerKey.isNotBlank() -> providerKey
        candidate.source == RemoteFoodSource.OPEN_FOOD_FACTS -> "OFF"
        candidate.source == RemoteFoodSource.USDA_FOODDATA_CENTRAL -> "USDA"
        candidate.source == RemoteFoodSource.NUTRITIONIX -> "Nutritionix"
        candidate.source == RemoteFoodSource.STATIC_SAMPLE -> "Sample"
        else -> "Online"
    }
}

fun deriveCompleteness(candidate: RemoteFoodCandidate): OnlineCandidateCompleteness {
    val populatedNutrients = listOf(
        candidate.caloriesKcalPer100g,
        candidate.proteinGPer100g,
        candidate.carbsGPer100g,
        candidate.fatGPer100g,
    ).count { value -> value != null }
    return when {
        populatedNutrients >= 4 -> OnlineCandidateCompleteness.COMPLETE
        populatedNutrients >= 2 -> OnlineCandidateCompleteness.PARTIAL
        else -> OnlineCandidateCompleteness.LIMITED
    }
}

fun deriveServingReviewStatus(servingSize: String?): OnlineServingReviewStatus {
    val normalized = servingSize
        ?.trim()
        ?.lowercase(Locale.ROOT)
        ?.takeIf { value -> value.isNotBlank() }
        ?: return OnlineServingReviewStatus.NEEDS_REVIEW

    if ('?' in normalized || normalized.contains("unknown")) {
        return OnlineServingReviewStatus.NEEDS_REVIEW
    }

    val hasDigit = normalized.any { character -> character.isDigit() }
    if (!hasDigit) {
        return OnlineServingReviewStatus.NEEDS_REVIEW
    }

    val hasKnownUnit = knownServingUnits.any { unit ->
        normalized.contains("\\b$unit\\b".toRegex(RegexOption.IGNORE_CASE))
    }
    return if (hasKnownUnit) {
        OnlineServingReviewStatus.OK
    } else {
        OnlineServingReviewStatus.NEEDS_REVIEW
    }
}
