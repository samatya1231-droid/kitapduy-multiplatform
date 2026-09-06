package com.semtinazizi.kitapduy.platform

import com.semtinazizi.kitapduy.data.SupabaseManager
import io.ktor.http.Url
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

object IosAuthenticationBridge {
    var open: ((String) -> Unit)? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val callbackLock = Mutex()

    fun fail(message: String) { AuthenticationMessages.error.value = message }

    fun handleCallback(url: String) {
        scope.launch {
            callbackLock.withLock {
                try {
                    val callback = Url(url)
                    if (callback.protocol.name != "kitapduy" || callback.host != "login-callback") return@withLock
                    check(callback.parameters["error"] == null) { "Giriş sağlayıcısı işlemi tamamlayamadı." }
                    val code = callback.parameters["code"]
                    check(!code.isNullOrBlank()) { "Giriş bağlantısı geçersiz veya süresi dolmuş. Lütfen yeniden giriş yapın." }
                    withTimeout(30_000) {
                        SupabaseManager.auth.awaitInitialization()
                        SupabaseManager.auth.exchangeCodeForSession(code)
                    }
                    AuthenticationMessages.error.value = null
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    // Never log callback URLs: they contain authentication codes.
                    fail("Giriş tamamlanamadı. İnternet bağlantınızı kontrol edip tekrar deneyin.")
                }
            }
        }
    }
}

actual fun openAuthenticationUrl(url: String) {
    val open = IosAuthenticationBridge.open
    if (open == null) IosAuthenticationBridge.fail("Giriş penceresi hazır değil. Lütfen tekrar deneyin.")
    else open(url)
}
