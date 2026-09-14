package com.semtinazizi.kitapduy.platform

import android.content.Intent
import android.net.Uri
import com.semtinazizi.kitapduy.MainActivity

actual fun openAuthenticationUrl(url: String) {
    val activity = MainActivity.instance
    if (activity == null) AuthenticationMessages.error.value = "Giriş penceresi açılamadı."
    else activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

// Android, Apple Sign In'i OAuth URL akışıyla yapıyor. Bu fonksiyon iOS'a özel.
actual fun triggerNativeAppleSignIn() { /* no-op on Android */ }
