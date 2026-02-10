package com.openfuel.app.domain.search

import com.openfuel.app.data.remote.ServingNutrientKind
import com.openfuel.app.data.remote.buildServingText
import com.openfuel.app.data.remote.normalizeServingText
import com.openfuel.app.data.remote.normalizeServingUnit
import com.openfuel.app.data.remote.per100EquivalentFromServing
import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.domain.model.RemoteFoodSource
import com.openfuel.app.domain.service.ProviderRequestType
import com.openfuel.app.domain.service.buildProviderCacheKey
import com.openfuel.app.domain.service.buildProviderDedupeKey
import com.openfuel.sharedcore.normalization.CoreProviderRequestType
import com.openfuel.sharedcore.normalization.CoreServingNutrientKind
import com.openfuel.sharedcore.normalization.buildNormalizedSqlLikePattern as coreBuildNormalizedSqlLikePattern
import com.openfuel.sharedcore.normalization.buildProviderCacheKey as coreBuildProviderCacheKey
import com.openfuel.sharedcore.normalization.buildProviderDedupeKey as coreBuildProviderDedupeKey
import com.openfuel.sharedcore.normalization.buildServingText as coreBuildServingText
import com.openfuel.sharedcore.normalization.normalizeProviderBarcode as coreNormalizeProviderBarcode
import com.openfuel.sharedcore.normalization.normalizeProviderText as coreNormalizeProviderText
import com.openfuel.sharedcore.normalization.normalizeSearchQuery as coreNormalizeSearchQuery
import com.openfuel.sharedcore.normalization.normalizeServingText as coreNormalizeServingText
import com.openfuel.sharedcore.normalization.normalizeServingUnit as coreNormalizeServingUnit
import com.openfuel.sharedcore.normalization.per100EquivalentFromServing as corePer100EquivalentFromServing
import com.openfuel.sharedcore.online.coreCandidateDecisionKey
import com.openfuel.sharedcore.online.deriveCoreCandidateTrustSignals
import org.junit.Assert.assertEquals
import org.junit.Test

class SharedCoreParityTest {
    @Test
    fun queryNormalization_matchesSharedCore() {
        val inputs = listOf(
            "Coke Zero 330ml",
            "Greek yoghurt 0 percent",
            "banana ×2",
            "  Oats, Milk + Honey  ",
        )

        inputs.forEach { input ->
            val appNormalized = normalizeSearchQuery(input)
            val coreNormalized = coreNormalizeSearchQuery(input)
            assertEquals(coreNormalized, appNormalized)

            val appPattern = buildNormalizedSqlLikePattern(appNormalized)
            val corePattern = coreBuildNormalizedSqlLikePattern(coreNormalized)
            assertEquals(corePattern, appPattern)
        }
    }

    @Test
    fun servingNormalization_matchesSharedCore() {
        val servingTextInput = "1 can(330ml)"
        assertEquals(coreNormalizeServingText(servingTextInput), normalizeServingText(servingTextInput))

        val servingUnitInput = "Millilitres"
        assertEquals(coreNormalizeServingUnit(servingUnitInput), normalizeServingUnit(servingUnitInput))

        assertEquals(
            coreBuildServingText(1.0, "can", 330.0),
            buildServingText(1.0, "can", 330.0),
        )

        assertEquals(
            corePer100EquivalentFromServing(
                nutrientValue = 80.0,
                nutrientKind = CoreServingNutrientKind.CALORIES,
                servingWeightGrams = 200.0,
                servingQuantity = 1.0,
                servingUnit = "serving",
            ),
            per100EquivalentFromServing(
                nutrientValue = 80.0,
                nutrientKind = ServingNutrientKind.CALORIES,
                servingWeightGrams = 200.0,
                servingQuantity = 1.0,
                servingUnit = "serving",
            ),
        )
    }

    @Test
    fun providerNormalization_matchesSharedCore() {
        val candidate = RemoteFoodCandidate(
            source = RemoteFoodSource.NUTRITIONIX,
            sourceId = "nix-123",
            providerKey = "nutritionix",
            barcode = " 012345 ",
            name = "Protein Bar",
            brand = "Acme",
            caloriesKcalPer100g = 380.0,
            proteinGPer100g = 12.0,
            carbsGPer100g = 45.0,
            fatGPer100g = 10.0,
            servingSize = "100 g",
        )

        assertEquals(coreNormalizeProviderText("  Foo   Bar  "), com.openfuel.app.domain.service.normalizeProviderText("  Foo   Bar  "))
        assertEquals(coreNormalizeProviderBarcode(" 012345 "), com.openfuel.app.domain.service.normalizeProviderBarcode(" 012345 "))
        assertEquals(coreBuildProviderDedupeKey(candidate.toCoreRemoteFoodCandidate()), buildProviderDedupeKey(candidate))

        assertEquals(
            coreBuildProviderCacheKey("usda_fdc", CoreProviderRequestType.TEXT_SEARCH, "  chicken breast  "),
            buildProviderCacheKey("usda_fdc", ProviderRequestType.TEXT_SEARCH, "  chicken breast  "),
        )
        assertEquals(
            coreBuildProviderCacheKey("open_food_facts", CoreProviderRequestType.BARCODE_LOOKUP, " 01234 "),
            buildProviderCacheKey("open_food_facts", ProviderRequestType.BARCODE_LOOKUP, " 01234 "),
        )
    }

    @Test
    fun trustSignalDerivation_matchesSharedCore() {
        val candidate = RemoteFoodCandidate(
            source = RemoteFoodSource.USDA_FOODDATA_CENTRAL,
            sourceId = "usda-42",
            providerKey = "usda_fdc",
            barcode = null,
            name = "Greek Yogurt",
            brand = null,
            caloriesKcalPer100g = 65.0,
            proteinGPer100g = 11.0,
            carbsGPer100g = 3.5,
            fatGPer100g = 0.2,
            servingSize = "170 g",
        )

        val appSignals = deriveOnlineCandidateTrustSignals(candidate)
        val coreSignals = deriveCoreCandidateTrustSignals(candidate.toCoreRemoteFoodCandidate())

        assertEquals(coreSignals.decisionKey, appSignals.decisionKey)
        assertEquals(coreSignals.provenanceLabel, appSignals.provenanceLabel)
        assertEquals(coreSignals.completeness.name, appSignals.completeness.name)
        assertEquals(coreSignals.servingReviewStatus.name, appSignals.servingReviewStatus.name)
        assertEquals(coreCandidateDecisionKey(candidate.toCoreRemoteFoodCandidate()), onlineCandidateDecisionKey(candidate))
    }
}
