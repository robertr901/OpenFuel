package com.openfuel.app.data.entitlement

data class BillingRefreshResult(
    val isEntitled: Boolean,
    val errorMessage: String? = null,
)

sealed class BillingPurchaseResult {
    data object Success : BillingPurchaseResult()
    data object Cancelled : BillingPurchaseResult()
    data class Error(val message: String) : BillingPurchaseResult()
}

interface BillingGateway {
    suspend fun refreshEntitlement(productId: String): BillingRefreshResult
    suspend fun launchPurchaseFlow(productId: String): BillingPurchaseResult
}
