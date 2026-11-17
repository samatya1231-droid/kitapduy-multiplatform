package com.semtinazizi.kitapduy.platform

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.autoreleasepool
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreFoundation.CFDataCreateWithBytesNoCopy
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFNumberCreate
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.kCFAllocatorNull
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFNumberIntType
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.CoreGraphics.CGImageRelease
import platform.ImageIO.CGImageSourceCreateThumbnailAtIndex
import platform.ImageIO.CGImageSourceCreateWithData
import platform.ImageIO.kCGImageSourceCreateThumbnailFromImageAlways
import platform.ImageIO.kCGImageSourceCreateThumbnailWithTransform
import platform.ImageIO.kCGImageSourceThumbnailMaxPixelSize
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation

internal actual suspend fun prepareProfilePhoto(
    bytes: ByteArray,
    extension: String,
    isCover: Boolean
): PreparedProfilePhoto = withContext(Dispatchers.Default) {
    val dimension = if (isCover) 1920 else 1080
    val quality = if (isCover) 0.8 else 0.85
    PreparedProfilePhoto(makeJpeg(bytes, dimension, quality), "jpg")
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun makeJpeg(bytes: ByteArray, maxDimension: Int, quality: Double): ByteArray = autoreleasepool {
    val imageError = "Fotoğraf hazırlanamadı. Başka bir fotoğraf seçip tekrar dene."
    require(bytes.isNotEmpty()) { imageError }
    bytes.usePinned { pinned ->
        val data = CFDataCreateWithBytesNoCopy(
            null, pinned.addressOf(0).reinterpret(), bytes.size.toLong(), kCFAllocatorNull
        ) ?: error(imageError)
        try {
            val source = CGImageSourceCreateWithData(data, null) ?: error(imageError)
            try {
                val options = thumbnailOptions(maxDimension) ?: error(imageError)
                val image = try {
                    CGImageSourceCreateThumbnailAtIndex(source, 0uL, options) ?: error(imageError)
                } finally {
                    CFRelease(options)
                }
                try {
                    val jpeg = UIImageJPEGRepresentation(UIImage.imageWithCGImage(image), quality) ?: error(imageError)
                    jpeg.bytes?.readBytes(jpeg.length.toInt()) ?: error(imageError)
                } finally {
                    CGImageRelease(image)
                }
            } finally {
                CFRelease(source)
            }
        } finally {
            CFRelease(data)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun thumbnailOptions(maxDimension: Int) = memScoped {
    val pixelSize = alloc<IntVar> { value = maxDimension }
    val limit = CFNumberCreate(null, kCFNumberIntType, pixelSize.ptr) ?: return@memScoped null
    try {
        CFDictionaryCreateMutable(
            null, 3, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr
        )?.also { options ->
            CFDictionarySetValue(options, kCGImageSourceThumbnailMaxPixelSize, limit)
            CFDictionarySetValue(options, kCGImageSourceCreateThumbnailFromImageAlways, kCFBooleanTrue)
            CFDictionarySetValue(options, kCGImageSourceCreateThumbnailWithTransform, kCFBooleanTrue)
        }
    } finally {
        CFRelease(limit)
    }
}
