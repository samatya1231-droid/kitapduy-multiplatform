package com.semtinazizi.kitapduy.platform

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Native consent options are hidden until a platform configures this bridge. */
object AdsPrivacyBridge {
    private val required = MutableStateFlow(false)
    val isRequired = required.asStateFlow()
    var showOptions: (() -> Unit)? = null

    fun updateRequired(value: Boolean) { required.value = value }
    fun openOptions() { showOptions?.invoke() }
}
