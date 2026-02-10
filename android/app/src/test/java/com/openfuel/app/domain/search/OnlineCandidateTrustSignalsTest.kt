package com.openfuel.app.domain.search

import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.domain.model.RemoteFoodSource
import org.junit.Assert.assertEquals
import org.junit.Test

class OnlineCandidateTrustSignalsTest {
    @Test
    fun deriveOnlineCandidateTrustSignals_returnsCompleteForFullyPopulatedNutrition() {
        val candidate = candidate(
            sourceId = "full",
            calories = 65.0,
            protein = 11.0,
            carbs = 3.5,
            fat = 0.2,
            servingSize = "170 g",
            providerKey = "usda_fdc",
        )

        val trustSignals = deriveOnlineCandidateTrustSignals(candidate)

        assertEquals("USDA", trustSignals.provenanceLabel)
        assertEquals(OnlineCandidateCompleteness.COMPLETE, trustSignals.completeness)
        assertEquals(OnlineServingReviewStatus.OK, trustSignals.servingReviewStatus)
    }

    @Test
    fun deriveOnlineCandidateTrustSignals_returnsPartialForMixedNutrition() {
        val candidate = candidate(
            sourceId = "partial",
            calories = 90.0,
            protein = 4.0,
            carbs = null,
            fat = null,
            servingSize = "1 can (330 ml)",
            providerKey = "open_food_facts",
        )

        val trustSignals = deriveOnlineCandidateTrustSignals(candidate)

        assertEquals("OFF", trustSignals.provenanceLabel)
        assertEquals(OnlineCandidateCompleteness.PARTIAL, trustSignals.completeness)
        assertEquals(OnlineServingReviewStatus.OK, trustSignals.servingReviewStatus)
    }

    @Test
    fun deriveOnlineCandidateTrustSignals_returnsLimitedAndNeedsReviewForSparseData() {
        val candidate = candidate(
            sourceId = "limited",
            calories = null,
            protein = null,
            carbs = null,
            fat = null,
            servingSize = null,
            providerKey = null,
        )

        val trustSignals = deriveOnlineCandidateTrustSignals(candidate)

        assertEquals("Nutritionix", trustSignals.provenanceLabel)
        assertEquals(OnlineCandidateCompleteness.LIMITED, trustSignals.completeness)
        assertEquals(OnlineServingReviewStatus.NEEDS_REVIEW, trustSignals.servingReviewStatus)
    }

    @Test
    fun deriveServingReviewStatus_marksUnknownServingAsNeedsReview() {
        assertEquals(
            OnlineServingReviewStatus.NEEDS_REVIEW,
            deriveServingReviewStatus("unknown serving size"),
        )
        assertEquals(
            OnlineServingReviewStatus.NEEDS_REVIEW,
            deriveServingReviewStatus("  ??? "),
        )
        assertEquals(
            OnlineServingReviewStatus.NEEDS_REVIEW,
            deriveServingReviewStatus("portion"),
        )
    }

    @Test
    fun onlineCandidateDecisionKey_isStable() {
        val candidate = candidate(sourceId = "abc-123")
        assertEquals("NUTRITIONIX:abc-123", onlineCandidateDecisionKey(candidate))
        assertEquals("NUTRITIONIX:abc-123", onlineCandidateDecisionKey(candidate))
    }

    private fun candidate(
        sourceId: String,
        calories: Double? = 0.0,
        protein: Double? = 0.0,
        carbs: Double? = 0.0,
        fat: Double? = 0.0,
        servingSize: String? = "100 g",
        providerKey: String? = "nutritionix",
    ): RemoteFoodCandidate {
        return RemoteFoodCandidate(
            source = RemoteFoodSource.NUTRITIONIX,
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
