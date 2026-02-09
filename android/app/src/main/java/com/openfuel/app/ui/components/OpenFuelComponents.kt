package com.openfuel.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.openfuel.app.ui.design.OFTypography
import com.openfuel.app.ui.design.ofSurfaceTokens
import com.openfuel.app.ui.theme.Dimens

@Composable
fun OFCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(Dimens.m),
    testTag: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val surfaces = ofSurfaceTokens()
    Card(
        modifier = modifier.withOptionalTestTag(testTag),
        shape = RoundedCornerShape(Dimens.cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = surfaces.card,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.s),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.s),
            content = content,
        )
    }
}

@Composable
fun OFSectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = OFTypography.sectionTitle(),
                fontWeight = FontWeight.SemiBold,
            )
            trailing?.invoke()
        }
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = OFTypography.caption(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun OFRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    contentDescription: String? = null,
    testTag: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    var rowModifier = modifier
        .fillMaxWidth()
        .withOptionalTestTag(testTag)
    if (!contentDescription.isNullOrBlank()) {
        rowModifier = rowModifier.semantics {
            this.contentDescription = contentDescription
        }
    }
    if (onClick != null) {
        rowModifier = rowModifier.clickable(
            role = Role.Button,
            onClick = onClick,
        )
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Dimens.iconM),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Dimens.xs),
        ) {
            Text(
                text = title,
                style = OFTypography.body(),
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = OFTypography.caption(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun OFEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    ctaLabel: String? = null,
    onCtaClick: (() -> Unit)? = null,
) {
    OFCard(modifier = modifier) {
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
        if (!ctaLabel.isNullOrBlank() && onCtaClick != null) {
            OFSecondaryButton(
                text = ctaLabel,
                onClick = onCtaClick,
            )
        }
    }
}

@Composable
fun OFMetricRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = OFTypography.body(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = OFTypography.body(),
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun OFPill(
    text: String,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    Surface(
        modifier = modifier.withOptionalTestTag(testTag),
        shape = RoundedCornerShape(Dimens.pillRadius),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = Dimens.sm, vertical = Dimens.xs),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
fun OFStatPill(
    text: String,
    modifier: Modifier = Modifier,
) {
    OFPill(
        text = text,
        modifier = modifier,
    )
}

@Composable
fun OFPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = Dimens.touchTargetMin)
            .withOptionalTestTag(testTag),
        enabled = enabled,
        shape = RoundedCornerShape(Dimens.sectionRadius),
        contentPadding = PaddingValues(horizontal = Dimens.m, vertical = Dimens.s),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun OFSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = Dimens.touchTargetMin)
            .withOptionalTestTag(testTag),
        enabled = enabled,
        shape = RoundedCornerShape(Dimens.sectionRadius),
        colors = ButtonDefaults.outlinedButtonColors(),
        contentPadding = PaddingValues(horizontal = Dimens.m, vertical = Dimens.s),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

private fun Modifier.withOptionalTestTag(testTag: String?): Modifier {
    return if (testTag.isNullOrBlank()) this else this.testTag(testTag)
}
