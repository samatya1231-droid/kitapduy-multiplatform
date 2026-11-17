package com.semtinazizi.kitapduy.platform

internal actual suspend fun prepareProfilePhoto(
    bytes: ByteArray,
    extension: String,
    isCover: Boolean
): PreparedProfilePhoto = PreparedProfilePhoto(bytes, extension)
