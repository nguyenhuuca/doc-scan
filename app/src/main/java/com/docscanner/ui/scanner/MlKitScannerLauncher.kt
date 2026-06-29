package com.docscanner.ui.scanner

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.docscanner.common.AppConfig
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

sealed class ScannerResult {
    data class Success(val imageUris: List<Uri>) : ScannerResult()
    object Cancelled : ScannerResult()
    data class Error(val message: String) : ScannerResult()
    object GmsUpdateRequired : ScannerResult()
}

@Composable
fun rememberMlKitScannerLauncher(
    onResult: (ScannerResult) -> Unit
): () -> Unit {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                val uris = scanResult?.pages?.mapNotNull { it.imageUri } ?: emptyList()
                if (uris.isEmpty()) {
                    onResult(ScannerResult.Error("No pages captured."))
                } else {
                    onResult(ScannerResult.Success(uris))
                }
            }
            Activity.RESULT_CANCELED -> onResult(ScannerResult.Cancelled)
            else -> onResult(ScannerResult.Error("Scan failed. Please try again."))
        }
    }

    return remember(context) {
        {
            val options = GmsDocumentScannerOptions.Builder()
                .setScannerMode(SCANNER_MODE_FULL)
                .setGalleryImportAllowed(true)
                .setPageLimit(AppConfig.MAX_PAGES)
                .setResultFormats(RESULT_FORMAT_JPEG)
                .build()

            val scanner: GmsDocumentScanner = GmsDocumentScanning.getClient(options)

            scanner.getStartScanIntent(context as Activity)
                .addOnSuccessListener { intentSender ->
                    launcher.launch(IntentSenderRequest.Builder(intentSender).build())
                }
                .addOnFailureListener { exception ->
                    // EC-4: Check if GMS update is required
                    val errorMessage = exception.message ?: ""
                    if (errorMessage.contains("update", ignoreCase = true) ||
                        errorMessage.contains("UserRecoverableException", ignoreCase = true)) {
                        onResult(ScannerResult.GmsUpdateRequired)
                    } else {
                        onResult(ScannerResult.Error("Scan failed. Please try again."))
                    }
                }
        }
    }
}
