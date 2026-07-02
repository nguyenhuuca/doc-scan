package com.docscanner.ui.edit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageProcessor {

    suspend fun rotateBitmap(bitmap: Bitmap, degrees: Float = 90f): Bitmap =
        withContext(Dispatchers.Default) {
            val matrix = Matrix().apply { postRotate(degrees) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }

    suspend fun adjustBrightness(bitmap: Bitmap, brightness: Float): Bitmap =
        withContext(Dispatchers.Default) {
            // brightness: -255f to +255f; 0 = no change
            val colorMatrix = ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, brightness,
                0f, 1f, 0f, 0f, brightness,
                0f, 0f, 1f, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            ))
            applyColorMatrix(bitmap, colorMatrix)
        }

    suspend fun adjustContrast(bitmap: Bitmap, contrast: Float): Bitmap =
        withContext(Dispatchers.Default) {
            // contrast: 0f to 2f; 1 = no change
            val translate = (-.5f * contrast + .5f) * 255f
            val colorMatrix = ColorMatrix(floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            applyColorMatrix(bitmap, colorMatrix)
        }

    // Combines brightness and contrast into a single matrix pass so both are always
    // applied fresh from a fixed base bitmap — chaining two separate passes on top of
    // an already-adjusted bitmap would compound the effect on every recompute.
    suspend fun adjustBrightnessContrast(bitmap: Bitmap, brightness: Float, contrast: Float): Bitmap =
        withContext(Dispatchers.Default) {
            val translate = (-.5f * contrast + .5f) * 255f + brightness
            val colorMatrix = ColorMatrix(floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            applyColorMatrix(bitmap, colorMatrix)
        }

    suspend fun toGrayscale(bitmap: Bitmap): Bitmap =
        withContext(Dispatchers.Default) {
            val colorMatrix = ColorMatrix().apply { setSaturation(0f) }
            applyColorMatrix(bitmap, colorMatrix)
        }

    private fun applyColorMatrix(bitmap: Bitmap, colorMatrix: ColorMatrix): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(colorMatrix) }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }
}
