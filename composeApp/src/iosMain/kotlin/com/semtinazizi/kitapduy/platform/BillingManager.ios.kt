package com.semtinazizi.kitapduy.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.StoreKit.*
import platform.Foundation.*
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class BillingManager {
    private val _isPremium = MutableStateFlow(false)
    actual val isPremium: StateFlow<Boolean> = _isPremium
    
    private var purchaseContinuation: kotlin.coroutines.Continuation<PurchaseResult>? = null

    private val transactionObserver = object : NSObject(), SKPaymentTransactionObserverProtocol {
        override fun paymentQueue(queue: SKPaymentQueue, updatedTransactions: List<*>) {
            for (transaction in updatedTransactions) {
                if (transaction is SKPaymentTransaction) {
                    when (transaction.transactionState) {
                        SKPaymentTransactionState.SKPaymentTransactionStatePurchased,
                        SKPaymentTransactionState.SKPaymentTransactionStateRestored -> {
                            _isPremium.value = true
                            SKPaymentQueue.defaultQueue().finishTransaction(transaction)
                            purchaseContinuation?.resume(PurchaseResult.SUCCESS)
                            purchaseContinuation = null
                        }
                        SKPaymentTransactionState.SKPaymentTransactionStateFailed -> {
                            SKPaymentQueue.defaultQueue().finishTransaction(transaction)
                            val error = transaction.error
                            if (error != null && error.code == 2L) {
                                purchaseContinuation?.resume(PurchaseResult.CANCELLED)
                            } else {
                                purchaseContinuation?.resume(PurchaseResult.ERROR)
                            }
                            purchaseContinuation = null
                        }
                        else -> {
                            // Diğer durumlar (Purchasing, Deferred) için bir şey yapmıyoruz.
                        }
                    }
                }
            }
        }

        override fun paymentQueueRestoreCompletedTransactionsFinished(queue: SKPaymentQueue) {
            // Restore başarılı oldu, eğer daha önce alınmış bir ürün varsa updatedTransactions içinde stateRestored olarak gelecektir.
        }

        override fun paymentQueue(queue: SKPaymentQueue, restoreCompletedTransactionsFailedWithError: NSError) {
            // Restore hatası
        }
    }

    init {
        SKPaymentQueue.defaultQueue().addTransactionObserver(transactionObserver)
    }

    actual suspend fun startSubscription(productId: String): PurchaseResult {
        if (!SKPaymentQueue.canMakePayments()) {
            return PurchaseResult.ERROR
        }

        return suspendCoroutine { continuation ->
            purchaseContinuation = continuation
            
            val productIdentifiers = setOf(productId)
            val request = SKProductsRequest(productIdentifiers)
            
            val requestDelegate = object : NSObject(), SKProductsRequestDelegateProtocol {
                override fun productsRequest(request: SKProductsRequest, didReceiveResponse: SKProductsResponse) {
                    val products = didReceiveResponse.products
                    if (products.isNotEmpty()) {
                        val product = products.first() as SKProduct
                        val payment = SKPayment.paymentWithProduct(product)
                        SKPaymentQueue.defaultQueue().addPayment(payment)
                    } else {
                        purchaseContinuation?.resume(PurchaseResult.ERROR)
                        purchaseContinuation = null
                    }
                }
                
                override fun request(request: SKRequest, didFailWithError: NSError) {
                    purchaseContinuation?.resume(PurchaseResult.ERROR)
                    purchaseContinuation = null
                }
            }
            
            request.delegate = requestDelegate
            request.start()
        }
    }

    actual suspend fun restorePurchases() {
        SKPaymentQueue.defaultQueue().restoreCompletedTransactions()
    }
}

actual fun createBillingManager(): BillingManager = BillingManager()
