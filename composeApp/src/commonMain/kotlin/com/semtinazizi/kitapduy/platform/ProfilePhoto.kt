package com.semtinazizi.kitapduy.platform

internal data class PreparedProfilePhoto(val bytes: ByteArray, val extension: String)

internal expect suspend fun prepareProfilePhoto(
    bytes: ByteArray,
    extension: String,
    isCover: Boolean
): PreparedProfilePhoto
