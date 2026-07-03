package com.docscanner.data.local.filesystem

import android.graphics.Bitmap
import com.docscanner.common.AppConfig
import com.docscanner.common.decodeSampledBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ThumbnailGenerator(private val filesDir: File) {

    private fun thumbnailFile(documentId: String): File {
        requireValidDocumentId(documentId)
        return File(filesDir, "documents/$documentId/thumbnail.jpg")
    }

    suspend fun generateThumbnail(
        documentId: String,
        sourceBitmap: Bitmap
    ): String = withContext(Dispatchers.IO) {
        val file = thumbnailFile(documentId)
        file.parentFile?.mkdirs()
        val scaled = withContext(Dispatchers.Default) { scaleThumbnail(sourceBitmap) }
        FileOutputStream(file).use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, AppConfig.THUMBNAIL_JPEG_QUALITY, out)
        }
        if (scaled !== sourceBitmap) scaled.recycle()
        file.absolutePath
    }

    suspend fun generateThumbnailFromPath(
        documentId: String,
        sourceImagePath: String
    ): String? {
        val maxSize = AppConfig.THUMBNAIL_MAX_SIZE
        val bitmap = withContext(Dispatchers.Default) {
            decodeSampledBitmap(sourceImagePath, maxSize, maxSize)
        } ?: return null
        val path = generateThumbnail(documentId, bitmap)
        bitmap.recycle()
        return path
    }

    private fun scaleThumbnail(bitmap: Bitmap): Bitmap {
        val maxSize = AppConfig.THUMBNAIL_MAX_SIZE
        if (bitmap.width == 0 || bitmap.height == 0) return bitmap
        if (bitmap.width <= maxSize && bitmap.height <= maxSize) return bitmap
        val scale = maxSize.toFloat() / maxOf(bitmap.width, bitmap.height)
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
