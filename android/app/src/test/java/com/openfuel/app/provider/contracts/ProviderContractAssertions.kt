package com.openfuel.app.provider.contracts

import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.domain.search.OnlineServingReviewStatus
import com.openfuel.app.domain.search.deriveOnlineCandidateTrustSignals
import com.openfuel.app.domain.search.onlineCandidateDecisionKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

open class ProviderContractAssertions {
    protected fun assertCommonInvariants(
        providerName: String,
        fixtureName: String,
        candidates: List<RemoteFoodCandidate>,
    ) {
        candidates.forEachIndexed { index, candidate ->
            val prefix = "[$providerName/$fixtureName][$index]"
            assertTrue("$prefix sourceId must be non-blank", candidate.sourceId.isNotBlank())
            assertTrue("$prefix name must be non-blank", candidate.name.isNotBlank())
            assertStringHygiene("$prefix name", candidate.name)
            candidate.brand?.let { brand ->
                assertStringHygiene("$prefix brand", brand)
            }
            assertNutrient("$prefix calories", candidate.caloriesKcalPer100g)
            assertNutrient("$prefix protein", candidate.proteinGPer100g)
            assertNutrient("$prefix carbs", candidate.carbsGPer100g)
            assertNutrient("$prefix fat", candidate.fatGPer100g)

            candidate.servingSize?.let { servingSize ->
                assertTrue("$prefix servingSize must be trimmed", servingSize == servingSize.trim())
                assertTrue("$prefix servingSize must be non-blank", servingSize.isNotBlank())
                assertTrue(
                    "$prefix servingSize should preserve parseable context",
                    servingSize.any { it.isDigit() } || servingSize.any { it.isLetter() },
                )
            }
        }
    }

    protected fun assertDeterministic(
        providerName: String,
        fixtureName: String,
        first: List<RemoteFoodCandidate>,
        second: List<RemoteFoodCandidate>,
    ) {
        assertEquals(
            "[$providerName/$fixtureName] mapping must be deterministic",
            first,
            second,
        )
    }

    protected fun assertTrustSignals(
        providerName: String,
        fixtureName: String,
        candidates: List<RemoteFoodCandidate>,
    ) {
        candidates.forEachIndexed { index, candidate ->
            val prefix = "[$providerName/$fixtureName][$index]"
            val trustSignals = deriveOnlineCandidateTrustSignals(candidate)
            assertEquals(
                "$prefix decision key must be stable",
                onlineCandidateDecisionKey(candidate),
                trustSignals.decisionKey,
            )
            assertTrue("$prefix provenance label must be non-blank", trustSignals.provenanceLabel.isNotBlank())
            if (candidate.servingSize.isNullOrBlank()) {
                assertEquals(
                    "$prefix missing serving must be marked needs review",
                    OnlineServingReviewStatus.NEEDS_REVIEW,
                    trustSignals.servingReviewStatus,
                )
            }
        }
    }

    private fun assertStringHygiene(label: String, value: String) {
        assertTrue("$label must be trimmed", value == value.trim())
        assertFalse("$label must not contain repeated spaces", value.contains("  "))
        assertFalse("$label must not start with punctuation", value.startsWithPunctuation())
        assertFalse("$label must not end with punctuation", value.endsWithPunctuation())
    }

    private fun assertNutrient(label: String, value: Double?) {
        if (value == null) return
        assertTrue("$label must be finite", value.isFinite())
        assertTrue("$label must be non-negative", value >= 0.0)
    }

    private fun String.startsWithPunctuation(): Boolean {
        return firstOrNull()?.let { ch -> !ch.isLetterOrDigit() } == true
    }

    private fun String.endsWithPunctuation(): Boolean {
        return lastOrNull()?.let { ch -> !ch.isLetterOrDigit() } == true
    }
}
