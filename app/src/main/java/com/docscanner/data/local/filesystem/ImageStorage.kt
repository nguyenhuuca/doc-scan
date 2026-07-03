package com.docscanner.data.local.filesystem

import android.graphics.Bitmap
import com.docscanner.common.AppConfig
import com.docscanner.common.decodeSampledBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ImageStorage(private val filesDir: File) {

    private fun documentPagesDir(documentId: String): File {
        requireValidDocumentId(documentId)
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
            withContext(Dispatchers.IO) { requireContainedImagePath(oldImagePath, filesDir).delete() }
        }
        return savePageImage(documentId, pageNumber, bitmap, updatedAt)
    }

    suspend fun deletePageImage(imagePath: String) = withContext(Dispatchers.IO) {
        requireContainedImagePath(imagePath, filesDir).delete()
    }

    suspend fun deleteDocumentImages(documentId: String) = withContext(Dispatchers.IO) {
        requireValidDocumentId(documentId)
        File(filesDir, "documents/$documentId").deleteRecursively()
    }

    suspend fun loadPageBitmap(imagePath: String): Bitmap? = withContext(Dispatchers.IO) {
        val file = requireContainedImagePath(imagePath, filesDir)
        // Cap at storage max to bound memory on large source files
        decodeSampledBitmap(file.path, AppConfig.IMAGE_MAX_WIDTH, AppConfig.IMAGE_MAX_HEIGHT)
    }

    suspend fun pageFileExists(imagePath: String): Boolean =
        withContext(Dispatchers.IO) { requireContainedImagePath(imagePath, filesDir).exists() }

    suspend fun storageUsedBytes(): Long = withContext(Dispatchers.IO) {
        val root = File(filesDir, "documents")
        if (!root.exists()) 0L
        else root.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

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
