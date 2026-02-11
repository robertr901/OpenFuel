package com.openfuel.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openfuel.app.ui.design.OFTypography
import com.openfuel.app.ui.design.ofStatusTokens
import com.openfuel.app.ui.theme.Dimens

enum class StatusTint {
    SUCCESS,
    WARN,
    OVER,
}

enum class PillKind {
    DEFAULT,
    SUCCESS,
    WARNING,
    OVER,
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    testTag: String? = null,
) {
    OFSectionHeader(
        title = title,
        subtitle = subtitle,
        modifier = modifier.withOptionalTestTag(testTag),
        trailing = trailing,
    )
}

@Composable
fun StandardCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(Dimens.m),
    testTag: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    OFCard(
        modifier = modifier,
        contentPadding = contentPadding,
        testTag = testTag,
        content = content,
    )
}

@Composable
fun ListRowCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    contentDescription: String? = null,
    testTag: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    StandardCard(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = Dimens.m, vertical = Dimens.sm),
        testTag = testTag,
    ) {
        OFRow(
            title = title,
            subtitle = subtitle,
            contentDescription = contentDescription,
            onClick = onClick,
            trailing = trailing,
        )
    }
}

@Composable
fun HeroMetric(
    value: String,
    unit: String,
    label: String,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    statusTint: StatusTint? = null,
    testTag: String? = null,
) {
    val statusColor = when (statusTint) {
        StatusTint.SUCCESS -> ofStatusTokens().successContent
        StatusTint.WARN -> ofStatusTokens().warningContent
        StatusTint.OVER -> ofStatusTokens().overContent
        null -> MaterialTheme.colorScheme.onSurface
    }

    StandardCard(
        modifier = modifier,
        testTag = testTag,
    ) {
        Text(
            text = label,
            style = OFTypography.label(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.xs),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = value,
                style = OFTypography.metricValue(),
                fontWeight = FontWeight.SemiBold,
                color = statusColor,
            )
            Text(
                text = unit,
                style = OFTypography.metricUnit(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        if (progress != null) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = statusColor,
            )
        }
    }
}

@Composable
fun MetricPill(
    text: String,
    kind: PillKind,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    val statusTokens = ofStatusTokens()
    val (containerColor, contentColor) = when (kind) {
        PillKind.DEFAULT -> Pair(
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        PillKind.SUCCESS -> Pair(statusTokens.successContainer, statusTokens.successContent)
        PillKind.WARNING -> Pair(statusTokens.warningContainer, statusTokens.warningContent)
        PillKind.OVER -> Pair(statusTokens.overContainer, statusTokens.overContent)
    }

    androidx.compose.material3.Surface(
        modifier = modifier.withOptionalTestTag(testTag),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(Dimens.pillRadius),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = Dimens.sm, vertical = Dimens.xs),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    primaryAction: (@Composable () -> Unit)? = null,
    testTag: String? = null,
) {
    StandardCard(
        modifier = modifier.withOptionalTestTag(testTag),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = title,
            style = OFTypography.sectionTitle(),
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = body,
            style = OFTypography.body(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        primaryAction?.invoke()
    }
}

private fun Modifier.withOptionalTestTag(testTag: String?): Modifier {
    return if (testTag.isNullOrBlank()) this else this.testTag(testTag)
}
