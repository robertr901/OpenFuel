package com.openfuel.app.ui.util

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

fun formatCalories(value: Double): String {
    return value.roundToInt().toString()
}

fun formatMacro(value: Double): String {
    return if (abs(value % 1.0) < 0.01) {
        value.roundToInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", value)
    }
}

fun formatQuantity(value: Double): String {
    return if (abs(value % 1.0) < 0.01) {
        value.roundToInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.2f", value)
    }
}
