package com.docscanner.ui.scanner

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.docscanner.R
import com.docscanner.ui.common.CameraPermissionState
import com.docscanner.ui.common.GmsChecker
import com.docscanner.ui.common.rememberCameraPermissionState
import com.docscanner.ui.scanner.components.ManualCropScreen
import com.docscanner.ui.scanner.components.PermissionRationaleScreen

@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel,
    onNavigateBack: () -> Unit,
    onScanComplete: (documentId: String) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val (permissionState, requestPermission) = rememberCameraPermissionState()
    var showGmsUpdateDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorDialogMessage by remember { mutableStateOf("") }

    LaunchedEffect(uiState.savedDocumentId) {
        uiState.savedDocumentId?.let { onScanComplete(it) }
    }

    LaunchedEffect(uiState.navigateBack) {
        if (uiState.navigateBack) onNavigateBack()
    }

    // Show error dialog instead of silently navigating back
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            errorDialogMessage = msg
            showErrorDialog = true
            viewModel.clearError()
        }
    }

    when (permissionState) {
        CameraPermissionState.Denied -> {
            LaunchedEffect(Unit) { requestPermission() }
        }
        CameraPermissionState.ShouldShowRationale -> {
            PermissionRationaleScreen(modifier = Modifier.fillMaxSize())
        }
        CameraPermissionState.Granted -> {
            val isGmsAvailable = remember(context) { GmsChecker.isGmsAvailable(context) }

            if (isGmsAvailable) {
                val scannerLauncher = rememberMlKitScannerLauncher { result ->
                    when (result) {
                        is ScannerResult.Success -> viewModel.onScanSuccess(result.imageUris, context)
                        is ScannerResult.Cancelled -> viewModel.onScanCancelled()
                        is ScannerResult.Error -> viewModel.onScanError(result.message)
                        is ScannerResult.GmsUpdateRequired -> showGmsUpdateDialog = true
                    }
                }

                LaunchedEffect(Unit) {
                    viewModel.checkStorageAndLaunch(context) {
                        scannerLauncher()
                    }
                }

                // Always show spinner — GMS screen is a transparent trampoline.
                // Scanner launches immediately; spinner covers blank-white gaps during:
                //   init, scanner-return window, and navigation fade-out.
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                ManualCropScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (showGmsUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showGmsUpdateDialog = false; onNavigateBack() },
            title = { Text(stringResource(R.string.update_required_title)) },
            text = { Text(stringResource(R.string.gms_update_required)) },
            confirmButton = {
                TextButton(onClick = { showGmsUpdateDialog = false; onNavigateBack() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false; onNavigateBack() },
            title = { Text(stringResource(R.string.scan_failed_title)) },
            text = { Text(errorDialogMessage) },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false; onNavigateBack() }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}
