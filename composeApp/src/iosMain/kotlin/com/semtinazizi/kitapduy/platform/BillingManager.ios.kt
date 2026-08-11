package com.semtinazizi.kitapduy.platform

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * iOS BillingManager Stub
 * Gerçek StoreKit entegrasyonu ileride buraya eklenecek.
 * Şimdilik her zaman isPremium = false döner — uygulama derlenebilir kalır.
 */
actual class BillingManager {
    private val _isPremium = MutableStateFlow(false)
    actual val isPremium: StateFlow<Boolean> = _isPremium

    actual suspend fun startSubscription(productId: String): PurchaseResult {
        // iOS StoreKit entegrasyonu ilerleyen aşamada eklenecek
        return PurchaseResult.ERROR
    }

    actual suspend fun restorePurchases() {
        // iOS StoreKit entegrasyonu ilerleyen aşamada eklenecek
    }
}

actual fun createBillingManager(): BillingManager = BillingManager()
