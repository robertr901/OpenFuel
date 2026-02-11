package com.openfuel.app.ui.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object OFSpacing {
    val x2: Dp = 2.dp
    val x4: Dp = 4.dp
    val x8: Dp = 8.dp
    val x12: Dp = 12.dp
    val x16: Dp = 16.dp
    val x24: Dp = 24.dp
    val x32: Dp = 32.dp
    val x40: Dp = 40.dp

    // Named aliases for consistent design-system usage across screens/components.
    val xxs: Dp = x2
    val xs: Dp = x4
    val sm: Dp = x8
    val md: Dp = x16
    val lg: Dp = x24
    val xl: Dp = x32
    val xxl: Dp = x40
}

object OFRadius {
    val small: Dp = 12.dp
    val medium: Dp = 16.dp
    val large: Dp = 20.dp
    val pill: Dp = 999.dp

    // Backwards-compatible aliases used by existing components.
    val card: Dp = large
    val control: Dp = medium
}

data class OFSurfaceTokens(
    val background: Color,
    val card: Color,
    val elevated: Color,
    val divider: Color,
)

@Composable
fun ofSurfaceTokens(): OFSurfaceTokens {
    val colors = MaterialTheme.colorScheme
    return OFSurfaceTokens(
        background = colors.background,
        card = colors.surfaceVariant.copy(alpha = 0.84f),
        elevated = colors.surface.copy(alpha = 0.94f),
        divider = colors.outlineVariant.copy(alpha = 0.38f),
    )
}

data class OFStatusTokens(
    val successContainer: Color,
    val successContent: Color,
    val warningContainer: Color,
    val warningContent: Color,
    val overContainer: Color,
    val overContent: Color,
)

@Composable
fun ofStatusTokens(): OFStatusTokens {
    val colors = MaterialTheme.colorScheme
    return OFStatusTokens(
        successContainer = colors.tertiaryContainer.copy(alpha = 0.72f),
        successContent = colors.onTertiaryContainer,
        warningContainer = colors.secondaryContainer.copy(alpha = 0.72f),
        warningContent = colors.onSecondaryContainer,
        overContainer = colors.errorContainer.copy(alpha = 0.82f),
        overContent = colors.onErrorContainer,
    )
}

object OFTypography {
    @Composable
    fun display(): TextStyle = MaterialTheme.typography.displaySmall.copy(
        fontWeight = FontWeight.SemiBold,
    )

    @Composable
    fun headline(): TextStyle = MaterialTheme.typography.titleLarge.copy(
        fontWeight = FontWeight.SemiBold,
    )

    @Composable
    fun sectionTitle(): TextStyle = MaterialTheme.typography.titleMedium.copy(
        fontWeight = FontWeight.SemiBold,
    )

    @Composable
    fun body(): TextStyle = MaterialTheme.typography.bodyMedium

    @Composable
    fun caption(): TextStyle = MaterialTheme.typography.bodySmall

    @Composable
    fun label(): TextStyle = MaterialTheme.typography.labelLarge

    @Composable
    fun metricValue(): TextStyle = MaterialTheme.typography.headlineMedium.copy(
        fontWeight = FontWeight.SemiBold,
    )

    @Composable
    fun metricUnit(): TextStyle = MaterialTheme.typography.labelLarge.copy(
        fontWeight = FontWeight.Medium,
    )
}
