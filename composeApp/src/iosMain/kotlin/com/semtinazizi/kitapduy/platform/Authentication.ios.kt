package com.semtinazizi.kitapduy.platform

import com.semtinazizi.kitapduy.data.SupabaseManager
import io.github.jan.supabase.auth.providers.Apple
import io.github.jan.supabase.auth.providers.builtin.IDToken
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
                    fail("Giriş tamamlanamadı. İnternet bağlantınızı kontrol edip tekrar deneyin.")
                }
            }
        }
    }
}

/**
 * Native Apple Sign In köprüsü.
 * Swift tarafı (AuthenticationService.swift) bu objeye lambda atar.
 * Lambda çağrıldığında Swift, ASAuthorizationController başlatır.
 * Kimlik doğrulama başarılı olunca Swift, handleAppleSignIn() fonksiyonunu çağırır.
 */
object IosAppleSignInBridge {
    /** Swift tarafından set edilir — ASAuthorizationController'ı başlatır */
    var startSignIn: (() -> Unit)? = null
    /** İlk Apple girişinde ASAuthorizationAppleIDCredential.fullName bilgisini taşır */
    var pendingFullName: String? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun fail(message: String) { AuthenticationMessages.error.value = message }

    /**
     * Swift, Apple kimlik doğrulaması başarılı olduğunda bu fonksiyonu çağırır.
     * identityToken: Apple'ın döndürdüğü JWT kimlik token'ı
     * nonce: SHA256 hash'lenmiş rastgele string (replay attack'e karşı)
     */
    fun handleAppleSignIn(identityToken: String, rawNonce: String) {
        val capturedFullName = pendingFullName?.trim()?.takeIf { it.isNotEmpty() }
        pendingFullName = null
        scope.launch {
            try {
                withTimeout(30_000) {
                    SupabaseManager.auth.awaitInitialization()
                    SupabaseManager.auth.signInWith(IDToken) {
                        idToken = identityToken
                        provider = Apple
                        nonce = rawNonce
                    }
                }
                val uid = SupabaseManager.auth.currentUserOrNull()?.id
                if (uid != null && capturedFullName != null) {
                    runCatching {
                        com.russhwolf.settings.Settings().putString("user_profile_name_$uid", capturedFullName)
                    }
                }
                AuthenticationMessages.error.value = null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                fail("Apple ile giriş tamamlanamadı. Lütfen tekrar deneyin.")
            }
        }
    }
}

actual fun openAuthenticationUrl(url: String) {
    val open = IosAuthenticationBridge.open
    if (open == null) IosAuthenticationBridge.fail("Giriş penceresi hazır değil. Lütfen tekrar deneyin.")
    else open(url)
}

/** iOS'ta native Apple Sign In akışını başlatır (ASAuthorizationController) */
actual fun triggerNativeAppleSignIn() {
    val start = IosAppleSignInBridge.startSignIn
    if (start == null) IosAppleSignInBridge.fail("Apple giriş servisi hazır değil. Lütfen tekrar deneyin.")
    else start()
}

