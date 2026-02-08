package com.openfuel.app.data.entitlement

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PlayBillingGateway(
    context: Context,
    private val currentActivityProvider: () -> Activity?,
) : BillingGateway, PurchasesUpdatedListener {
    private companion object {
        private const val PURCHASE_TIMEOUT_MS = 120_000L
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private val connectionMutex = Mutex()
    private val purchaseMutex = Mutex()
    private val purchaseEvents = Channel<BillingPurchaseResult>(capacity = Channel.CONFLATED)

    override suspend fun refreshEntitlement(productId: String): BillingRefreshResult {
        val connectionError = ensureReady()
        if (connectionError != null) {
            return BillingRefreshResult(
                isEntitled = false,
                errorMessage = connectionError,
            )
        }

        val queryResult = queryPurchases()
        if (queryResult.first.responseCode != BillingClient.BillingResponseCode.OK) {
            return BillingRefreshResult(
                isEntitled = false,
                errorMessage = mapBillingError(
                    result = queryResult.first,
                    fallback = "Could not refresh purchases right now.",
                ),
            )
        }

        val purchases = queryResult.second
        val hasEntitlement = purchases.any { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                purchase.products.contains(productId)
        }
        acknowledgeUnacknowledgedPurchases(purchases, productId)
        return BillingRefreshResult(isEntitled = hasEntitlement)
    }

    override suspend fun launchPurchaseFlow(productId: String): BillingPurchaseResult {
        return purchaseMutex.withLock {
            val connectionError = ensureReady()
            if (connectionError != null) {
                return@withLock BillingPurchaseResult.Error(connectionError)
            }

            val productDetails = queryProductDetails(productId)
                ?: return@withLock BillingPurchaseResult.Error("Pro purchase is unavailable right now.")

            val activity = currentActivityProvider()
                ?: return@withLock BillingPurchaseResult.Error(
                    "Unable to start purchase without an active screen.",
                )

            clearPurchaseEvents()
            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .build(),
                    ),
                )
                .build()
            val launchResult = billingClient.launchBillingFlow(activity, flowParams)
            if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
                return@withLock BillingPurchaseResult.Error(
                    mapBillingError(
                        result = launchResult,
                        fallback = "Could not start purchase flow.",
                    ),
                )
            }

            return@withLock withTimeoutOrNull(PURCHASE_TIMEOUT_MS) {
                purchaseEvents.receive()
            } ?: BillingPurchaseResult.Error("Purchase timed out. Please try again.")
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?,
    ) {
        val result = when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> BillingPurchaseResult.Success
            BillingClient.BillingResponseCode.USER_CANCELED -> BillingPurchaseResult.Cancelled
            else -> BillingPurchaseResult.Error(
                mapBillingError(
                    result = billingResult,
                    fallback = "Purchase failed. Please try again.",
                ),
            )
        }
        purchaseEvents.trySend(result)
    }

    private suspend fun ensureReady(): String? {
        return connectionMutex.withLock {
            if (billingClient.isReady) {
                return@withLock null
            }

            val result = startConnection()
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                null
            } else {
                mapBillingError(
                    result = result,
                    fallback = "Billing is not available on this device.",
                )
            }
        }
    }

    private suspend fun startConnection(): BillingResult {
        return suspendCoroutine { continuation ->
            billingClient.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingServiceDisconnected() {
                        // Connection is retried on next operation.
                    }

                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        continuation.resume(billingResult)
                    }
                },
            )
        }
    }

    private suspend fun queryPurchases(): Pair<BillingResult, List<Purchase>> {
        return suspendCoroutine { continuation ->
            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build(),
            ) { billingResult, purchases ->
                continuation.resume(billingResult to purchases)
            }
        }
    }

    private suspend fun queryProductDetails(productId: String): ProductDetails? {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(productId)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val queryParams = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        val result = suspendCoroutine<Pair<BillingResult, List<ProductDetails>>> { continuation ->
            billingClient.queryProductDetailsAsync(queryParams) { billingResult, details ->
                continuation.resume(billingResult to details)
            }
        }
        if (result.first.responseCode != BillingClient.BillingResponseCode.OK) {
            return null
        }
        return result.second.firstOrNull()
    }

    private fun clearPurchaseEvents() {
        while (purchaseEvents.tryReceive().isSuccess) {
            // Drain stale events before launching a new purchase flow.
        }
    }

    private suspend fun acknowledgeUnacknowledgedPurchases(
        purchases: List<Purchase>,
        productId: String,
    ) {
        purchases
            .asSequence()
            .filter { purchase ->
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    !purchase.isAcknowledged &&
                    purchase.products.contains(productId)
            }
            .forEach { purchase ->
                suspendCoroutine { continuation ->
                    billingClient.acknowledgePurchase(
                        AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build(),
                    ) {
                        continuation.resume(Unit)
                    }
                }
            }
    }

    private fun mapBillingError(
        result: BillingResult,
        fallback: String,
    ): String {
        return when (result.responseCode) {
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
            BillingClient.BillingResponseCode.NETWORK_ERROR,
            BillingClient.BillingResponseCode.SERVICE_TIMEOUT -> {
                "Billing service is unavailable. Check connection and try again."
            }
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                "Billing is not available on this device."
            }
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> {
                "Pro purchase is not available in this region or account."
            }
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> {
                "Billing configuration is invalid for this build."
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                "Purchase cancelled."
            }
            else -> fallback
        }
    }
}
