package com.docscanner.common

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
