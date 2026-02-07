package com.openfuel.app.domain.util

object EntryValidation {
    fun isValidQuantity(quantity: Double): Boolean {
        return quantity.isFinite() && quantity > 0.0
    }

    fun nonNegative(value: Double): Double {
        return if (value.isFinite() && value > 0.0) value else 0.0
    }
}
