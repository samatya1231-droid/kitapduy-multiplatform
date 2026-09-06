package com.semtinazizi.kitapduy.platform

import android.app.Activity
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Google Play Billing implementasyonu.
 * Premium abonelik durumunu [isPremium] StateFlow üzerinden yayar.
 */
actual class BillingManager(private val activity: Activity) : PurchasesUpdatedListener {

    private val _isPremium = MutableStateFlow(false)
    actual val isPremium: StateFlow<Boolean> = _isPremium

    private var billingClient: BillingClient = BillingClient.newBuilder(activity)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private var productDetailsList: List<ProductDetails> = emptyList()

    // Abonelik ürün ID'leri (Google Play Console'da tanımlanmış)
    private val subscriptionIds = listOf("premium_monthly", "premium_yearly")

    init {
        connectAndQuery()
    }

    private fun connectAndQuery() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails()
                    checkExistingPurchases()
                }
            }
            override fun onBillingServiceDisconnected() {
                // Bağlantı kesilirse yeniden bağlan
                connectAndQuery()
            }
        })
    }

    private fun queryProductDetails() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(subscriptionIds.map { id ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(id)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            })
            .build()

        billingClient.queryProductDetailsAsync(params) { _, details ->
            productDetailsList = details
        }
    }

    private fun checkExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasActiveSub = purchases.any { purchase ->
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                _isPremium.value = hasActiveSub
            }
        }
    }

    actual suspend fun loadProducts(): List<SubscriptionProduct> {
        kotlinx.coroutines.withTimeout(20_000) {
            while (productDetailsList.isEmpty()) kotlinx.coroutines.delay(200)
        }
        return productDetailsList.mapNotNull { product ->
            val phases = product.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList
            val renewal = phases?.lastOrNull() ?: return@mapNotNull null
            val period = when (renewal.billingPeriod) {
                "P1M" -> "1 ay"
                "P3M" -> "3 ay"
                "P1Y" -> "1 yıl"
                else -> renewal.billingPeriod
            }
            SubscriptionProduct(product.productId, product.name, renewal.formattedPrice, period)
        }
    }

    actual suspend fun startSubscription(productId: String): PurchaseResult {
        val productDetails = productDetailsList.find { it.productId == productId }
            ?: return PurchaseResult.ERROR

        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
            ?: return PurchaseResult.ERROR

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        val result = billingClient.launchBillingFlow(activity, flowParams)
        return when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> PurchaseResult.SUCCESS
            BillingClient.BillingResponseCode.USER_CANCELED -> PurchaseResult.CANCELLED
            else -> PurchaseResult.ERROR
        }
    }

    actual suspend fun restorePurchases() {
        checkExistingPurchases()
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    _isPremium.value = true
                    // Satın alımı onayla (acknowledge)
                    if (!purchase.isAcknowledged) {
                        val params = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                        billingClient.acknowledgePurchase(params) {}
                    }
                }
            }
        } else if (result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Kullanıcı iptal etti, herhangi bir şey yapmaya gerek yok
        }
    }

    fun release() {
        billingClient.endConnection()
    }
}

actual fun createBillingManager(): BillingManager {
    error("createBillingManager() Android'de Activity context gerektiriyor. Doğrudan BillingManager(activity) kullanın.")
}
