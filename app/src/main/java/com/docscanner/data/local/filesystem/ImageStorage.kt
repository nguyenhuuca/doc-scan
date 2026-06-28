package com.docscanner.data.local.filesystem

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ImageStorage(private val filesDir: File) {

    private fun documentPagesDir(documentId: String): File =
        File(filesDir, "documents/$documentId/pages").also { it.mkdirs() }

    private fun pageFilename(pageNumber: Int, updatedAt: Long): String =
        "page_%03d_%d.jpg".format(pageNumber, updatedAt)

    suspend fun savePageImage(
        documentId: String,
        pageNumber: Int,
        bitmap: Bitmap,
        updatedAt: Long
    ): String = withContext(Dispatchers.IO) {
        val dir = documentPagesDir(documentId)
        val filename = pageFilename(pageNumber, updatedAt)
        val file = File(dir, filename)
        val scaled = downscaleIfNeeded(bitmap)
        FileOutputStream(file).use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        if (scaled !== bitmap) scaled.recycle()
        file.absolutePath
    }

    suspend fun updatePageImage(
        documentId: String,
        pageNumber: Int,
        bitmap: Bitmap,
        oldImagePath: String?,
        updatedAt: Long
    ): String = withContext(Dispatchers.IO) {
        if (oldImagePath != null) {
            File(oldImagePath).delete()
        }
        savePageImage(documentId, pageNumber, bitmap, updatedAt)
    }

    suspend fun deletePageImage(imagePath: String) = withContext(Dispatchers.IO) {
        File(imagePath).delete()
    }

    suspend fun deleteDocumentImages(documentId: String) = withContext(Dispatchers.IO) {
        File(filesDir, "documents/$documentId").deleteRecursively()
    }

    fun loadPageBitmap(imagePath: String): Bitmap? =
        BitmapFactory.decodeFile(imagePath)

    fun pageFileExists(imagePath: String): Boolean = File(imagePath).exists()

    private fun downscaleIfNeeded(bitmap: Bitmap): Bitmap {
        val maxWidth = 2480
        val maxHeight = 3508
        if (bitmap.width <= maxWidth && bitmap.height <= maxHeight) return bitmap
        val widthRatio = maxWidth.toFloat() / bitmap.width
        val heightRatio = maxHeight.toFloat() / bitmap.height
        val scale = minOf(widthRatio, heightRatio)
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
