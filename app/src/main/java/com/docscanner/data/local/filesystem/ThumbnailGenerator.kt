package com.docscanner.data.local.filesystem

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.docscanner.common.AppConfig
import com.docscanner.common.calcInSampleSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ThumbnailGenerator(private val filesDir: File) {

    private fun requireValidId(documentId: String) {
        require(documentId.matches(Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))) {
            "Invalid document ID format"
        }
    }

    private fun thumbnailFile(documentId: String): File {
        requireValidId(documentId)
        return File(filesDir, "documents/$documentId/thumbnail.jpg")
    }

    suspend fun generateThumbnail(
        documentId: String,
        sourceBitmap: Bitmap
    ): String = withContext(Dispatchers.IO) {
        val file = thumbnailFile(documentId)
        file.parentFile?.mkdirs()
        val scaled = scaleThumbnail(sourceBitmap)
        FileOutputStream(file).use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, AppConfig.THUMBNAIL_JPEG_QUALITY, out)
        }
        if (scaled !== sourceBitmap) scaled.recycle()
        file.absolutePath
    }

    suspend fun generateThumbnailFromPath(
        documentId: String,
        sourceImagePath: String
    ): String? = withContext(Dispatchers.IO) {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(sourceImagePath, opts)
        if (opts.outWidth <= 0) return@withContext null
        val maxSize = AppConfig.THUMBNAIL_MAX_SIZE
        opts.inSampleSize = calcInSampleSize(opts.outWidth, opts.outHeight, maxSize, maxSize)
        opts.inJustDecodeBounds = false
        val bitmap = BitmapFactory.decodeFile(sourceImagePath, opts) ?: return@withContext null
        val path = generateThumbnail(documentId, bitmap)
        bitmap.recycle()
        path
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
