package com.docscanner.ui.common

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

enum class CameraPermissionState {
    Granted, Denied, ShouldShowRationale
}

@Composable
fun rememberCameraPermissionState(): Pair<CameraPermissionState, () -> Unit> {
    val context = LocalContext.current
    var state by remember {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        mutableStateOf(if (granted) CameraPermissionState.Granted else CameraPermissionState.Denied)
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        state = if (isGranted) CameraPermissionState.Granted else CameraPermissionState.Denied
    }

    return state to { launcher.launch(Manifest.permission.CAMERA) }
}
