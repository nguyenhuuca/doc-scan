package com.docscanner.ui.edit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import com.docscanner.R
import com.docscanner.common.AppConfig
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    viewModel: EditViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var brightnessValue by remember { mutableFloatStateOf(0f) }
    var contrastValue by remember { mutableFloatStateOf(1f) }
    var isGrayscale by remember { mutableStateOf(false) }

    BackHandler(enabled = uiState.hasUnsavedChanges) {
        showDiscardDialog = true
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            viewModel.clearSaved()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_page_title, viewModel.pageIndex + 1)) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.hasUnsavedChanges) showDiscardDialog = true
                        else onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = uiState.canUndo
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(R.string.undo))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Image preview — 65% of screen height
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.65f),
                contentAlignment = Alignment.Center
            ) {
                uiState.currentBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.page_preview),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (uiState.isProcessing) {
                    CircularProgressIndicator()
                }
            }

            // Controls — remaining 35%
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.35f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Rotate button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.rotate() },
                        enabled = !uiState.isProcessing
                    ) {
                        Icon(Icons.Default.Rotate90DegreesCw, contentDescription = stringResource(R.string.rotate_90))
                    }
                    Text(stringResource(R.string.rotate_90), style = MaterialTheme.typography.bodyLarge)
                }

                // Brightness slider
                Column {
                    Text(stringResource(R.string.brightness_label, brightnessValue.toInt()), style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = brightnessValue,
                        onValueChange = {
                            brightnessValue = it
                            viewModel.adjustBrightness(it)
                        },
                        valueRange = AppConfig.EDIT_BRIGHTNESS_MIN..AppConfig.EDIT_BRIGHTNESS_MAX,
                        enabled = !uiState.isProcessing
                    )
                }

                // Contrast slider
                Column {
                    Text(
                        stringResource(R.string.contrast_label, String.format(Locale.US, "%.2f", contrastValue)),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Slider(
                        value = contrastValue,
                        onValueChange = {
                            contrastValue = it
                            viewModel.adjustContrast(it)
                        },
                        valueRange = AppConfig.EDIT_CONTRAST_MIN..AppConfig.EDIT_CONTRAST_MAX,
                        enabled = !uiState.isProcessing
                    )
                }

                // Grayscale toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.grayscale), modifier = Modifier.weight(1f))
                    Switch(
                        checked = isGrayscale,
                        onCheckedChange = {
                            isGrayscale = it
                            if (it) viewModel.toGrayscale()
                        },
                        enabled = !uiState.isProcessing
                    )
                }

                // Save button
                FilledTonalButton(
                    onClick = { viewModel.save() },
                    enabled = uiState.hasUnsavedChanges && !uiState.isProcessing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.discard_changes_title)) },
            text = { Text(stringResource(R.string.unsaved_changes_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onNavigateBack()
                }) { Text(stringResource(R.string.discard)) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
