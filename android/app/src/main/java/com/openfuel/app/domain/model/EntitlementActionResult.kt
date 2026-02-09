package com.openfuel.app.domain.model

sealed class EntitlementActionResult {
    data class Success(val message: String) : EntitlementActionResult()
    data class Error(val message: String) : EntitlementActionResult()
    data object Cancelled : EntitlementActionResult()
}
