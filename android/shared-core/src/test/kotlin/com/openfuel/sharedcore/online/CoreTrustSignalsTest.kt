package com.openfuel.sharedcore.online

import com.openfuel.sharedcore.model.CoreRemoteFoodCandidate
import org.junit.Assert.assertEquals
import org.junit.Test

class CoreTrustSignalsTest {
    @Test
    fun deriveCoreCandidateTrustSignals_returnsCompleteForFullyPopulatedNutrition() {
        val candidate = candidate(
            sourceId = "full",
            calories = 65.0,
            protein = 11.0,
            carbs = 3.5,
            fat = 0.2,
            servingSize = "170 g",
            providerKey = "usda_fdc",
        )

        val trustSignals = deriveCoreCandidateTrustSignals(candidate)

        assertEquals("USDA", trustSignals.provenanceLabel)
        assertEquals(CoreCandidateCompleteness.COMPLETE, trustSignals.completeness)
        assertEquals(CoreServingReviewStatus.OK, trustSignals.servingReviewStatus)
    }

    @Test
    fun deriveCoreCandidateTrustSignals_returnsPartialForMixedNutrition() {
        val candidate = candidate(
            sourceId = "partial",
            calories = 90.0,
            protein = 4.0,
            carbs = null,
            fat = null,
            servingSize = "1 can (330 ml)",
            providerKey = "open_food_facts",
        )

        val trustSignals = deriveCoreCandidateTrustSignals(candidate)

        assertEquals("OFF", trustSignals.provenanceLabel)
        assertEquals(CoreCandidateCompleteness.PARTIAL, trustSignals.completeness)
        assertEquals(CoreServingReviewStatus.OK, trustSignals.servingReviewStatus)
    }

    @Test
    fun deriveCoreCandidateTrustSignals_returnsLimitedAndNeedsReviewForSparseData() {
        val candidate = candidate(
            sourceId = "limited",
            calories = null,
            protein = null,
            carbs = null,
            fat = null,
            servingSize = null,
            providerKey = null,
        )

        val trustSignals = deriveCoreCandidateTrustSignals(candidate)

        assertEquals("Nutritionix", trustSignals.provenanceLabel)
        assertEquals(CoreCandidateCompleteness.LIMITED, trustSignals.completeness)
        assertEquals(CoreServingReviewStatus.NEEDS_REVIEW, trustSignals.servingReviewStatus)
    }

    @Test
    fun deriveCoreServingReviewStatus_marksUnknownServingAsNeedsReview() {
        assertEquals(
            CoreServingReviewStatus.NEEDS_REVIEW,
            deriveCoreServingReviewStatus("unknown serving size"),
        )
        assertEquals(
            CoreServingReviewStatus.NEEDS_REVIEW,
            deriveCoreServingReviewStatus("  ??? "),
        )
        assertEquals(
            CoreServingReviewStatus.NEEDS_REVIEW,
            deriveCoreServingReviewStatus("portion"),
        )
    }

    @Test
    fun coreCandidateDecisionKey_isStable() {
        val candidate = candidate(sourceId = "abc-123")
        assertEquals("NUTRITIONIX:abc-123", coreCandidateDecisionKey(candidate))
        assertEquals("NUTRITIONIX:abc-123", coreCandidateDecisionKey(candidate))
    }

    private fun candidate(
        sourceId: String,
        calories: Double? = 0.0,
        protein: Double? = 0.0,
        carbs: Double? = 0.0,
        fat: Double? = 0.0,
        servingSize: String? = "100 g",
        providerKey: String? = "nutritionix",
    ): CoreRemoteFoodCandidate {
        return CoreRemoteFoodCandidate(
            source = "NUTRITIONIX",
            sourceId = sourceId,
            providerKey = providerKey,
            barcode = null,
            name = "Candidate $sourceId",
            brand = "Brand",
            caloriesKcalPer100g = calories,
            proteinGPer100g = protein,
            carbsGPer100g = carbs,
            fatGPer100g = fat,
            servingSize = servingSize,
        )
    }
}
