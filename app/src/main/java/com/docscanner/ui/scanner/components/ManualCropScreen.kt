package com.docscanner.ui.scanner.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.docscanner.ui.scanner.ScannerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@Composable
fun ManualCropScreen(
    viewModel: ScannerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var quad by remember { mutableStateOf<Quad?>(null) }
    var imageDisplaySize by remember { mutableStateOf(IntSize.Zero) }
    var isProcessing by remember { mutableStateOf(false) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    if (capturedBitmap == null) {
        // Camera preview phase
        Box(modifier = modifier) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                onPreviewCreated = { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture
                        )
                    }, ContextCompat.getMainExecutor(context))
                }
            )

            Button(
                onClick = {
                    val outputFile = File(context.cacheDir, "capture_${UUID.randomUUID()}.jpg")
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
                    imageCapture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                scope.launch {
                                    val bitmap = withContext(Dispatchers.IO) {
                                        BitmapFactory.decodeFile(outputFile.absolutePath)
                                    }
                                    outputFile.delete()
                                    bitmap?.let {
                                        capturedBitmap = it
                                        quad = PerspectiveTransform.defaultQuad(it.width.toFloat(), it.height.toFloat())
                                    }
                                }
                            }
                            override fun onError(exception: ImageCaptureException) {
                                // Capture failed — stay on preview
                            }
                        }
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                Text("Capture")
            }
        }
    } else {
        // Crop adjustment phase
        val bitmap = capturedBitmap!!
        val currentQuad = quad!!

        Column(modifier = modifier) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onSizeChanged { imageDisplaySize = it }
            ) {
                androidx.compose.foundation.Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Captured image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                CornerDragOverlay(
                    quad = currentQuad,
                    onQuadChanged = { quad = it },
                    modifier = Modifier.fillMaxSize()
                )
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { capturedBitmap = null; quad = null },
                    modifier = Modifier.weight(1f)
                ) { Text("Retake") }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {
                        scope.launch {
                            isProcessing = true
                            val cropped = PerspectiveTransform.transform(bitmap, currentQuad)
                            viewModel.onManualCropComplete(cropped, context)
                            isProcessing = false
                        }
                    },
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f)
                ) { Text("Use Photo") }
            }
        }
    }
}
