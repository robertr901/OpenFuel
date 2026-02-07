package com.openfuel.app.ui.util

private val decimalInputPattern = Regex("^\\d+(?:[\\.,]\\d+)?$")

fun parseDecimalInput(raw: String): Double? {
    val normalized = raw.trim()
    if (normalized.isEmpty()) {
        return null
    }
    if (!decimalInputPattern.matches(normalized)) {
        return null
    }
    return normalized.replace(',', '.').toDoubleOrNull()
}
