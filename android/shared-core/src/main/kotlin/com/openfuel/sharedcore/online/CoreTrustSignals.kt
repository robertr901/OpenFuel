package com.openfuel.sharedcore.online

import com.openfuel.sharedcore.model.CoreRemoteFoodCandidate
import java.util.Locale

enum class CoreCandidateCompleteness {
    COMPLETE,
    PARTIAL,
    LIMITED,
}

enum class CoreServingReviewStatus {
    OK,
    NEEDS_REVIEW,
}

data class CoreCandidateTrustSignals(
    val decisionKey: String,
    val provenanceLabel: String,
    val completeness: CoreCandidateCompleteness,
    val servingReviewStatus: CoreServingReviewStatus,
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

fun coreCandidateDecisionKey(candidate: CoreRemoteFoodCandidate): String {
    return "${candidate.source}:${candidate.sourceId}"
}

fun deriveCoreCandidateTrustSignals(
    candidate: CoreRemoteFoodCandidate,
): CoreCandidateTrustSignals {
    return CoreCandidateTrustSignals(
        decisionKey = coreCandidateDecisionKey(candidate),
        provenanceLabel = deriveCoreProvenanceLabel(candidate),
        completeness = deriveCoreCompleteness(candidate),
        servingReviewStatus = deriveCoreServingReviewStatus(candidate.servingSize),
    )
}

fun deriveCoreProvenanceLabel(candidate: CoreRemoteFoodCandidate): String {
    val providerKey = candidate.providerKey.orEmpty()
    return when {
        providerKey.equals("open_food_facts", ignoreCase = true) -> "OFF"
        providerKey.equals("usda_fdc", ignoreCase = true) -> "USDA"
        providerKey.equals("nutritionix", ignoreCase = true) -> "Nutritionix"
        providerKey.equals("static_sample", ignoreCase = true) -> "Sample"
        providerKey.isNotBlank() -> providerKey
        candidate.source.equals("OPEN_FOOD_FACTS", ignoreCase = true) -> "OFF"
        candidate.source.equals("USDA_FOODDATA_CENTRAL", ignoreCase = true) -> "USDA"
        candidate.source.equals("NUTRITIONIX", ignoreCase = true) -> "Nutritionix"
        candidate.source.equals("STATIC_SAMPLE", ignoreCase = true) -> "Sample"
        else -> "Online"
    }
}

fun deriveCoreCompleteness(candidate: CoreRemoteFoodCandidate): CoreCandidateCompleteness {
    val populatedNutrients = listOf(
        candidate.caloriesKcalPer100g,
        candidate.proteinGPer100g,
        candidate.carbsGPer100g,
        candidate.fatGPer100g,
    ).count { value -> value != null }
    return when {
        populatedNutrients >= 4 -> CoreCandidateCompleteness.COMPLETE
        populatedNutrients >= 2 -> CoreCandidateCompleteness.PARTIAL
        else -> CoreCandidateCompleteness.LIMITED
    }
}

fun deriveCoreServingReviewStatus(servingSize: String?): CoreServingReviewStatus {
    val normalized = servingSize
        ?.trim()
        ?.lowercase(Locale.ROOT)
        ?.takeIf { value -> value.isNotBlank() }
        ?: return CoreServingReviewStatus.NEEDS_REVIEW

    if ('?' in normalized || normalized.contains("unknown")) {
        return CoreServingReviewStatus.NEEDS_REVIEW
    }

    val hasDigit = normalized.any { character -> character.isDigit() }
    if (!hasDigit) {
        return CoreServingReviewStatus.NEEDS_REVIEW
    }

    val hasKnownUnit = knownServingUnits.any { unit ->
        normalized.contains("\\b$unit\\b".toRegex(RegexOption.IGNORE_CASE))
    }
    return if (hasKnownUnit) {
        CoreServingReviewStatus.OK
    } else {
        CoreServingReviewStatus.NEEDS_REVIEW
    }
}
