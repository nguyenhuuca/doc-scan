package com.docscanner.ui.scanner.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import android.graphics.PointF

@Composable
fun CornerDragOverlay(
    quad: Quad,
    onQuadChanged: (Quad) -> Unit,
    modifier: Modifier = Modifier
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .onSizeChanged { canvasSize = it }
            .pointerInput(quad) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val x = change.position.x
                    val y = change.position.y
                    val handleRadius = 48.dp.toPx()

                    val corners = listOf(
                        quad.topLeft, quad.topRight, quad.bottomRight, quad.bottomLeft
                    )
                    val closestIndex = corners.indexOfMinBy { corner ->
                        val dx = x - corner.x - dragAmount.x
                        val dy = y - corner.y - dragAmount.y
                        dx * dx + dy * dy
                    }

                    val newQuad = when (closestIndex) {
                        0 -> quad.copy(topLeft = PointF(quad.topLeft.x + dragAmount.x, quad.topLeft.y + dragAmount.y))
                        1 -> quad.copy(topRight = PointF(quad.topRight.x + dragAmount.x, quad.topRight.y + dragAmount.y))
                        2 -> quad.copy(bottomRight = PointF(quad.bottomRight.x + dragAmount.x, quad.bottomRight.y + dragAmount.y))
                        3 -> quad.copy(bottomLeft = PointF(quad.bottomLeft.x + dragAmount.x, quad.bottomLeft.y + dragAmount.y))
                        else -> quad
                    }
                    onQuadChanged(newQuad)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val tl = Offset(quad.topLeft.x, quad.topLeft.y)
            val tr = Offset(quad.topRight.x, quad.topRight.y)
            val br = Offset(quad.bottomRight.x, quad.bottomRight.y)
            val bl = Offset(quad.bottomLeft.x, quad.bottomLeft.y)

            // Draw quadrilateral outline
            drawLine(Color.Yellow, tl, tr, strokeWidth = 3.dp.toPx())
            drawLine(Color.Yellow, tr, br, strokeWidth = 3.dp.toPx())
            drawLine(Color.Yellow, br, bl, strokeWidth = 3.dp.toPx())
            drawLine(Color.Yellow, bl, tl, strokeWidth = 3.dp.toPx())

            // Draw corner handles (48dp min tap area)
            val handleRadius = 16.dp.toPx()
            listOf(tl, tr, br, bl).forEach { point ->
                drawCircle(Color.White, handleRadius, point)
                drawCircle(Color.Yellow, handleRadius - 4.dp.toPx(), point)
            }
        }
    }
}

private fun <T> List<T>.indexOfMinBy(selector: (T) -> Float): Int {
    var minIndex = 0
    var minValue = selector(this[0])
    for (i in 1 until size) {
        val v = selector(this[i])
        if (v < minValue) { minValue = v; minIndex = i }
    }
    return minIndex
}
