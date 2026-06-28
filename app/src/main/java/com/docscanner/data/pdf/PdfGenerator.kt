package com.docscanner.data.pdf

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfGenerator(private val cacheDir: File) {

    companion object {
        private const val PAGE_WIDTH_PT = 595
        private const val PAGE_HEIGHT_PT = 842
        private const val MARGIN_PT = 10
        private const val DRAWABLE_WIDTH = PAGE_WIDTH_PT - 2 * MARGIN_PT
        private const val DRAWABLE_HEIGHT = PAGE_HEIGHT_PT - 2 * MARGIN_PT
    }

    suspend fun generatePdf(
        documentId: String,
        pageImagePaths: List<String>
    ): File = withContext(Dispatchers.IO) {
        val exportDir = File(cacheDir, "export").also { it.mkdirs() }
        val outputFile = File(exportDir, "${documentId}_${System.currentTimeMillis()}.pdf")

        val pdfDocument = PdfDocument()
        try {
            pageImagePaths.forEachIndexed { index, imagePath ->
                val bitmap = loadBitmap(imagePath) ?: return@forEachIndexed
                try {
                    val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_PT, PAGE_HEIGHT_PT, index + 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    drawBitmapOnPage(page.canvas, bitmap)
                    pdfDocument.finishPage(page)
                } finally {
                    bitmap.recycle()
                }
            }

            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
        } finally {
            pdfDocument.close()
        }

        outputFile
    }

    private fun drawBitmapOnPage(canvas: Canvas, bitmap: Bitmap) {
        val drawableRect = RectF(
            MARGIN_PT.toFloat(),
            MARGIN_PT.toFloat(),
            (PAGE_WIDTH_PT - MARGIN_PT).toFloat(),
            (PAGE_HEIGHT_PT - MARGIN_PT).toFloat()
        )

        val bitmapWidth = bitmap.width.toFloat()
        val bitmapHeight = bitmap.height.toFloat()

        val scaleX = DRAWABLE_WIDTH.toFloat() / bitmapWidth
        val scaleY = DRAWABLE_HEIGHT.toFloat() / bitmapHeight
        val scale = minOf(scaleX, scaleY)

        val scaledWidth = bitmapWidth * scale
        val scaledHeight = bitmapHeight * scale

        val left = MARGIN_PT + (DRAWABLE_WIDTH - scaledWidth) / 2f
        val top = MARGIN_PT + (DRAWABLE_HEIGHT - scaledHeight) / 2f

        val matrix = Matrix()
        matrix.setScale(scale, scale)
        matrix.postTranslate(left, top)

        canvas.drawBitmap(bitmap, matrix, null)
    }

    private fun loadBitmap(imagePath: String): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(imagePath, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return null

        // Sample down if very large to stay within PDF page size
        val sampleSize = calculateSampleSize(options.outWidth, options.outHeight)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        return BitmapFactory.decodeFile(imagePath, decodeOptions)
    }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        while (width / sampleSize > PAGE_WIDTH_PT * 2 || height / sampleSize > PAGE_HEIGHT_PT * 2) {
            sampleSize *= 2
        }
        return sampleSize
    }
}
