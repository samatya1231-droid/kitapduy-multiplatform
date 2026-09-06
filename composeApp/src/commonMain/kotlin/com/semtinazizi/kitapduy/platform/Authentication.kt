package com.semtinazizi.kitapduy.platform

import kotlinx.coroutines.flow.MutableStateFlow

object AuthenticationMessages {
    val error = MutableStateFlow<String?>(null)
}

expect fun openAuthenticationUrl(url: String)
