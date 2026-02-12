package com.openfuel.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.openfuel.app.domain.quality.FoodDataQualityLevel
import com.openfuel.app.domain.quality.FoodDataQualityReason
import com.openfuel.app.domain.quality.FoodDataQualitySignals
import com.openfuel.app.ui.theme.Dimens

@Composable
fun FoodTrustCueRow(
    signals: FoodDataQualitySignals,
    onReviewAndFix: (() -> Unit)?,
    modifier: Modifier = Modifier,
    testTagPrefix: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .withOptionalTestTag(testTagPrefix, "trust_cue"),
        verticalArrangement = Arrangement.spacedBy(Dimens.xs),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.xs)) {
            OFPill(
                text = when (signals.level) {
                    FoodDataQualityLevel.COMPLETE -> "Complete"
                    FoodDataQualityLevel.NEEDS_REVIEW -> "Needs review"
                },
                modifier = Modifier.withOptionalTestTag(testTagPrefix, "trust_level"),
            )
        }
        if (signals.needsReview) {
            Text(
                text = signals.toGuidanceCopy(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (onReviewAndFix != null) {
                TextButton(
                    onClick = onReviewAndFix,
                    modifier = Modifier.withOptionalTestTag(testTagPrefix, "review_fix"),
                ) {
                    Text("Review and fix")
                }
            }
        }
    }
}

private fun FoodDataQualitySignals.toGuidanceCopy(): String {
    return when {
        reasons.contains(FoodDataQualityReason.REPORTED_INCORRECT) ->
            "This item was marked incorrect on this device."
        reasons.contains(FoodDataQualityReason.UNKNOWN_CALORIES) &&
            reasons.contains(FoodDataQualityReason.UNKNOWN_MACROS) ->
            "Nutrition values are incomplete."
        reasons.contains(FoodDataQualityReason.UNKNOWN_CALORIES) ->
            "Calories are unknown."
        reasons.contains(FoodDataQualityReason.UNKNOWN_MACROS) ->
            "Macros are incomplete."
        else -> "Nutrition values may need review."
    }
}

private fun Modifier.withOptionalTestTag(
    prefix: String?,
    suffix: String,
): Modifier {
    return if (prefix.isNullOrBlank()) {
        this
    } else {
        testTag("${prefix}_$suffix")
    }
}
