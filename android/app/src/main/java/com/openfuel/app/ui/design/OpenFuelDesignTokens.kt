package com.openfuel.app.ui.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object OFSpacing {
    val x4: Dp = 4.dp
    val x8: Dp = 8.dp
    val x12: Dp = 12.dp
    val x16: Dp = 16.dp
    val x24: Dp = 24.dp
    val x32: Dp = 32.dp
}

object OFRadius {
    val card: Dp = 20.dp
    val control: Dp = 16.dp
    val pill: Dp = 999.dp
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

object OFTypography {
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
}
