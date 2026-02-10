package com.openfuel.sharedcore.online

import com.openfuel.sharedcore.model.CoreRemoteFoodCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreOnlineSelectionTest {
    @Test
    fun mergeCandidates_whenOneSourceOnly_marksSingleSourceResult() {
        val candidate = candidate(sourceId = "off-1", providerKey = "open_food_facts")

        val result = mergeCandidates(
            providerCandidates = listOf(
                CoreProviderCandidates(
                    providerId = "open_food_facts",
                    candidates = listOf(candidate),
                ),
            ),
            priorityByProviderId = mapOf("open_food_facts" to 0),
        )

        val merged = result.candidates.single()
        val decision = result.candidateDecisions.getValue(coreCandidateDecisionKey(merged))
        assertEquals(CoreCandidateSelectionReason.SINGLE_SOURCE_RESULT, decision.reason)
        assertEquals("open_food_facts", merged.providerKey)
    }

    @Test
    fun mergeCandidates_whenSameBarcode_prefersRicherPayloadAndMarksBarcodeMatch() {
        val providerA = candidate(
            source = "OPEN_FOOD_FACTS",
            sourceId = "off-1",
            providerKey = "open_food_facts",
            barcode = "0123456789",
            protein = null,
            carbs = null,
            fat = null,
        )
        val providerB = candidate(
            source = "NUTRITIONIX",
            sourceId = "nix-1",
            providerKey = "nutritionix",
            barcode = "0123456789",
            protein = 20.0,
            carbs = 30.0,
            fat = 10.0,
        )

        val result = mergeCandidates(
            providerCandidates = listOf(
                CoreProviderCandidates("open_food_facts", listOf(providerA)),
                CoreProviderCandidates("nutritionix", listOf(providerB)),
            ),
            priorityByProviderId = mapOf(
                "open_food_facts" to 0,
                "nutritionix" to 1,
            ),
        )

        assertEquals(1, result.candidates.size)
        val selected = result.candidates.single()
        assertEquals("nutritionix", selected.providerKey)
        val decision = result.candidateDecisions.getValue(coreCandidateDecisionKey(selected))
        assertEquals(CoreCandidateSelectionReason.BARCODE_MATCH, decision.reason)
        assertEquals(listOf("open_food_facts", "nutritionix"), decision.contributingProviderIds)
    }

    @Test
    fun mergeCandidates_whenRichnessEqual_prefersProviderPriority() {
        val providerA = candidate(
            source = "USDA_FOODDATA_CENTRAL",
            sourceId = "usda-1",
            providerKey = "usda_fdc",
            barcode = null,
            name = "Almond Milk",
            serving = "250 ml",
        )
        val providerB = candidate(
            source = "NUTRITIONIX",
            sourceId = "nix-1",
            providerKey = "nutritionix",
            barcode = null,
            name = "Almond Milk",
            serving = "250 ml",
        )

        val result = mergeCandidates(
            providerCandidates = listOf(
                CoreProviderCandidates("nutritionix", listOf(providerB)),
                CoreProviderCandidates("usda_fdc", listOf(providerA)),
            ),
            priorityByProviderId = mapOf(
                "usda_fdc" to 0,
                "nutritionix" to 1,
            ),
        )

        val selected = result.candidates.single()
        assertEquals("usda_fdc", selected.providerKey)
        val decision = result.candidateDecisions.getValue(coreCandidateDecisionKey(selected))
        assertEquals(CoreCandidateSelectionReason.PREFERRED_SOURCE, decision.reason)
        assertTrue(decision.contributingProviderIds.contains("nutritionix"))
    }

    private fun candidate(
        source: String = "OPEN_FOOD_FACTS",
        sourceId: String,
        providerKey: String?,
        barcode: String? = null,
        name: String = "Protein Bar",
        serving: String = "100 g",
        calories: Double? = 350.0,
        protein: Double? = 12.0,
        carbs: Double? = 40.0,
        fat: Double? = 10.0,
    ): CoreRemoteFoodCandidate {
        return CoreRemoteFoodCandidate(
            source = source,
            sourceId = sourceId,
            providerKey = providerKey,
            barcode = barcode,
            name = name,
            brand = "Acme",
            caloriesKcalPer100g = calories,
            proteinGPer100g = protein,
            carbsGPer100g = carbs,
            fatGPer100g = fat,
            servingSize = serving,
        )
    }
}
