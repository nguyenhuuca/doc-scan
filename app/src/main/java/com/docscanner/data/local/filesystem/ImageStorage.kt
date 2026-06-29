package com.docscanner.data.local.filesystem

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.docscanner.common.AppConfig
import com.docscanner.common.calcInSampleSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ImageStorage(private val filesDir: File) {

    private fun requireValidId(documentId: String) {
        require(documentId.matches(Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))) {
            "Invalid document ID format"
        }
    }

    private fun documentPagesDir(documentId: String): File {
        requireValidId(documentId)
        return File(filesDir, "documents/$documentId/pages").also { it.mkdirs() }
    }

    private fun pageFilename(pageNumber: Int, updatedAt: Long): String =
        "page_%03d_%d.jpg".format(pageNumber, updatedAt)

    suspend fun savePageImage(
        documentId: String,
        pageNumber: Int,
        bitmap: Bitmap,
        updatedAt: Long
    ): String {
        val scaled = downscaleIfNeeded(bitmap)  // CPU on Default
        return withContext(Dispatchers.IO) {
            val dir = documentPagesDir(documentId)
            val filename = pageFilename(pageNumber, updatedAt)
            val file = File(dir, filename)
            FileOutputStream(file).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, AppConfig.IMAGE_JPEG_QUALITY, out)
            }
            if (scaled !== bitmap) scaled.recycle()
            file.absolutePath
        }
    }

    suspend fun updatePageImage(
        documentId: String,
        pageNumber: Int,
        bitmap: Bitmap,
        oldImagePath: String?,
        updatedAt: Long
    ): String {
        if (oldImagePath != null) {
            withContext(Dispatchers.IO) { File(oldImagePath).delete() }
        }
        return savePageImage(documentId, pageNumber, bitmap, updatedAt)
    }

    suspend fun deletePageImage(imagePath: String) = withContext(Dispatchers.IO) {
        File(imagePath).delete()
    }

    suspend fun deleteDocumentImages(documentId: String) = withContext(Dispatchers.IO) {
        requireValidId(documentId)
        File(filesDir, "documents/$documentId").deleteRecursively()
    }

    suspend fun loadPageBitmap(imagePath: String): Bitmap? = withContext(Dispatchers.IO) {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(imagePath, opts)
        if (opts.outWidth <= 0) return@withContext null
        // Cap at storage max to bound memory on large source files
        opts.inSampleSize = calcInSampleSize(opts.outWidth, opts.outHeight, AppConfig.IMAGE_MAX_WIDTH, AppConfig.IMAGE_MAX_HEIGHT)
        opts.inJustDecodeBounds = false
        BitmapFactory.decodeFile(imagePath, opts)
    }

    suspend fun pageFileExists(imagePath: String): Boolean =
        withContext(Dispatchers.IO) { File(imagePath).exists() }

    private suspend fun downscaleIfNeeded(bitmap: Bitmap): Bitmap {
        val maxWidth = AppConfig.IMAGE_MAX_WIDTH
        val maxHeight = AppConfig.IMAGE_MAX_HEIGHT
        if (bitmap.width <= maxWidth && bitmap.height <= maxHeight) return bitmap
        val widthRatio = maxWidth.toFloat() / bitmap.width
        val heightRatio = maxHeight.toFloat() / bitmap.height
        val scale = minOf(widthRatio, heightRatio)
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()
        return withContext(Dispatchers.Default) {
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        }
    }
}
