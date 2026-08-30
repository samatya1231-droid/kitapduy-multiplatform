@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)
package com.semtinazizi.kitapduy.platform

/**
 * Kotlin/Native global unhandled exception hook.
 *
 * iOS'ta arka plan GCD thread'lerinde (com.apple.root.utility-qos vb.) yakalananmayan
 * bir Kotlin exception doğrudan abort() -> SIGABRT crash'e yol açar.
 *
 * Bu hook kurulduğunda, yakalanmayan tüm Kotlin exception'lar loglanır ama
 * uygulamayı çökerTMEZ.
 */
fun installKotlinExceptionHook() {
    setUnhandledExceptionHook { throwable ->
        // Sadece logla, uygulamayı çökertme
        println("=== KitapDuy: Unhandled Kotlin exception caught by hook ===")
        println("Type: ${throwable::class.simpleName}")
        println("Message: ${throwable.message}")
        println("=== End of exception ===")
        // Burada rethrow YAPILMIYOR - bu uygulamanın çökmesini önler
    }
    println("KitapDuy: Kotlin exception hook installed successfully")
}
