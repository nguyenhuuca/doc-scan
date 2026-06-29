package com.docscanner.ui.scanner.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import com.docscanner.common.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Quad(
    val topLeft: PointF,
    val topRight: PointF,
    val bottomRight: PointF,
    val bottomLeft: PointF
)

object PerspectiveTransform {

    suspend fun transform(source: Bitmap, quad: Quad, outputWidth: Int = AppConfig.IMAGE_MAX_WIDTH, outputHeight: Int = AppConfig.IMAGE_MAX_HEIGHT): Bitmap =
        withContext(Dispatchers.Default) {
            val srcPoints = floatArrayOf(
                quad.topLeft.x, quad.topLeft.y,
                quad.topRight.x, quad.topRight.y,
                quad.bottomRight.x, quad.bottomRight.y,
                quad.bottomLeft.x, quad.bottomLeft.y
            )
            val dstPoints = floatArrayOf(
                0f, 0f,
                outputWidth.toFloat(), 0f,
                outputWidth.toFloat(), outputHeight.toFloat(),
                0f, outputHeight.toFloat()
            )

            val matrix = Matrix()
            matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)

            val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            canvas.drawBitmap(source, matrix, Paint(Paint.ANTI_ALIAS_FLAG))
            output
        }

    fun defaultQuad(imageWidth: Float, imageHeight: Float): Quad {
        val margin = minOf(imageWidth, imageHeight) * 0.05f
        return Quad(
            topLeft = PointF(margin, margin),
            topRight = PointF(imageWidth - margin, margin),
            bottomRight = PointF(imageWidth - margin, imageHeight - margin),
            bottomLeft = PointF(margin, imageHeight - margin)
        )
    }
}
