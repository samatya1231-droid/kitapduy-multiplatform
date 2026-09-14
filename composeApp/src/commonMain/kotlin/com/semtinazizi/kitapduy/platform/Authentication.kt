package com.semtinazizi.kitapduy.platform

import kotlinx.coroutines.flow.MutableStateFlow

object AuthenticationMessages {
    val error = MutableStateFlow<String?>(null)
}

expect fun openAuthenticationUrl(url: String)

/**
 * iOS'ta native Apple Sign In (ASAuthorizationController) başlatır.
 * Android'de no-op; Android tarafı OAuth URL flow'unu kullanır.
 */
expect fun triggerNativeAppleSignIn()
