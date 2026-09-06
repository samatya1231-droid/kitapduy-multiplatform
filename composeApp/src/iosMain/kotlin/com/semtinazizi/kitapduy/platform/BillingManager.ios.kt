package com.semtinazizi.kitapduy.platform

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Swift calls these entry points on the main thread. */
object IosBillingBridge {
    var loadProducts: ((String) -> Unit)? = null
    var purchase: ((String, String) -> Unit)? = null
    var restore: ((String) -> Unit)? = null
    internal val premium = MutableStateFlow(false)
    private var nextRequestId = 0L
    private val pending = mutableMapOf<String, CompletableDeferred<BillingReply>>()
    private val json = Json { ignoreUnknownKeys = true }

    fun updateEntitlement(active: Boolean) { premium.value = active }

    fun complete(requestId: String, responseJson: String) {
        val request = pending.remove(requestId) ?: return
        try {
            request.complete(json.decodeFromString<BillingReply>(responseJson))
        } catch (error: Exception) {
            request.completeExceptionally(IllegalStateException("Mağaza yanıtı okunamadı."))
        }
    }

    internal suspend fun request(action: (String) -> Unit): BillingReply = withContext(Dispatchers.Main) {
        val requestId = (++nextRequestId).toString()
        val result = CompletableDeferred<BillingReply>()
        pending[requestId] = result
        try {
            action(requestId)
            withTimeout(180_000) { result.await() }.also {
                if (it.status == "error") error(it.message ?: "Mağaza işlemi tamamlanamadı.")
            }
        } finally {
            pending.remove(requestId)
            result.cancel()
        }
    }
}

@Serializable
internal data class BillingReply(
    val status: String,
    val products: List<SubscriptionProduct> = emptyList(),
    val message: String? = null
)

actual class BillingManager {
    actual val isPremium: StateFlow<Boolean> = IosBillingBridge.premium

    actual suspend fun loadProducts(): List<SubscriptionProduct> = IosBillingBridge.request { id ->
        checkNotNull(IosBillingBridge.loadProducts) { "App Store bağlantısı hazır değil." }(id)
    }.products

    actual suspend fun startSubscription(productId: String): PurchaseResult {
        val reply = IosBillingBridge.request { id ->
            checkNotNull(IosBillingBridge.purchase) { "App Store bağlantısı hazır değil." }(id, productId)
        }
        return when (reply.status) {
            "success" -> PurchaseResult.SUCCESS
            "cancelled" -> PurchaseResult.CANCELLED
            "pending" -> error("Satın alma onay bekliyor. Onaylandığında Premium otomatik açılacak.")
            else -> PurchaseResult.ERROR
        }
    }

    actual suspend fun restorePurchases() {
        IosBillingBridge.request { id ->
            checkNotNull(IosBillingBridge.restore) { "App Store bağlantısı hazır değil." }(id)
        }
    }
}

actual fun createBillingManager(): BillingManager = BillingManager()
