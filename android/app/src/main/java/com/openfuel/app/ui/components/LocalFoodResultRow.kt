package com.openfuel.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.openfuel.app.domain.quality.classifyFoodItemQuality
import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.model.MealType
import com.openfuel.app.ui.theme.Dimens
import com.openfuel.app.ui.util.formatCalories
import com.openfuel.app.ui.util.formatMacro

@Composable
fun LocalFoodResultRow(
    food: FoodItem,
    sourceLabel: String,
    onLog: (MealType) -> Unit,
    onOpenPortion: () -> Unit,
    modifier: Modifier = Modifier,
    initialMealType: MealType = MealType.BREAKFAST,
    testTagPrefix: String? = null,
) {
    var selectedMeal by rememberSaveable(food.id) { mutableStateOf(initialMealType) }
    val qualitySignals = classifyFoodItemQuality(food)

    StandardCard(
        modifier = modifier
            .fillMaxWidth()
            .withOptionalTestTag(testTagPrefix, "row"),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.sm),
        ) {
            OFRow(
                title = food.name,
                subtitle = food.brand?.takeIf { it.isNotBlank() },
                trailing = { OFPill(text = sourceLabel) },
            )
            androidx.compose.material3.Text(
                text = "${formatCalories(food.caloriesKcal)} kcal · " +
                    "${formatMacro(food.proteinG)}p ${formatMacro(food.carbsG)}c ${formatMacro(food.fatG)}f",
                style = instrumentTextStyle(),
            )
            FoodTrustCueRow(
                signals = qualitySignals,
                onReviewAndFix = if (qualitySignals.needsReview) onOpenPortion else null,
                testTagPrefix = testTagPrefix,
            )
            MealTypeDropdown(
                selected = selectedMeal,
                onSelected = { selectedMeal = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OFPrimaryButton(
                    text = "Log",
                    onClick = { onLog(selectedMeal) },
                    modifier = Modifier
                        .weight(1f)
                        .withOptionalTestTag(testTagPrefix, "log"),
                )
                OFSecondaryButton(
                    text = "Portion",
                    onClick = onOpenPortion,
                    modifier = Modifier
                        .weight(1f)
                        .withOptionalTestTag(testTagPrefix, "portion"),
                )
            }
        }
    }
}

@Composable
private fun instrumentTextStyle() = MaterialTheme.typography.labelLarge.copy(
    fontWeight = FontWeight.Medium,
    fontFeatureSettings = "tnum",
)

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
