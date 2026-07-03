package com.docscanner.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory

fun calcInSampleSize(width: Int, height: Int, maxWidth: Int, maxHeight: Int): Int {
    if (width <= 0 || height <= 0) return 1
    var inSampleSize = 1
    if (height > maxHeight || width > maxWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while (halfHeight / inSampleSize >= maxHeight || halfWidth / inSampleSize >= maxWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

/**
 * Two-pass decode: bounds-only first to pick a sample size, then the real decode.
 * Returns null when the file is missing or not a decodable image.
 * Blocking — callers must dispatch to Dispatchers.IO/Default themselves.
 */
fun decodeSampledBitmap(imagePath: String, maxWidth: Int, maxHeight: Int): Bitmap? {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(imagePath, opts)
    if (opts.outWidth <= 0) return null
    opts.inSampleSize = calcInSampleSize(opts.outWidth, opts.outHeight, maxWidth, maxHeight)
    opts.inJustDecodeBounds = false
    return BitmapFactory.decodeFile(imagePath, opts)
}

/** Same two-pass decode for in-memory JPEG/PNG bytes. */
fun decodeSampledBitmap(bytes: ByteArray, maxWidth: Int, maxHeight: Int): Bitmap? {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    if (opts.outWidth <= 0) return null
    opts.inSampleSize = calcInSampleSize(opts.outWidth, opts.outHeight, maxWidth, maxHeight)
    opts.inJustDecodeBounds = false
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
}
