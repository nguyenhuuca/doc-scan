package com.docscanner.ui.viewer

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.docscanner.domain.model.Page
import com.docscanner.ui.viewer.components.MissingPagePlaceholder
import com.docscanner.ui.viewer.components.ReorderableThumbnailList

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewerScreen(
    viewModel: DocumentViewerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToScanner: (documentId: String) -> Unit,
    onNavigateToEdit: (documentId: String, pageIndex: Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showPageMenu by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            // Read bytes on main thread immediately — URI permission scoped to delivery window
            val items = uris.mapNotNull { uri ->
                val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull()
                val bytes = runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }.getOrNull()
                if (bytes != null) bytes to mime else null
            }
            viewModel.importFromGallery(items)
        }
    }

    val pagerState = rememberPagerState(pageCount = { uiState.pages.size })

    LaunchedEffect(uiState.documentDeleted) {
        if (uiState.documentDeleted) onNavigateBack()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.exportedFile) {
        uiState.exportedFile?.let { file ->
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share PDF"))
            viewModel.clearExportedFile()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.document?.name ?: "Document") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Per-page three-dot menu
                    Box {
                        IconButton(onClick = { showPageMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(expanded = showPageMenu, onDismissRequest = { showPageMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Export PDF") },
                                onClick = {
                                    viewModel.exportPdf()
                                    showPageMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Import from Gallery") },
                                onClick = {
                                    galleryLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                    showPageMenu = false
                                },
                                enabled = uiState.canAddPage
                            )
                            val currentPage = uiState.pages.getOrNull(pagerState.currentPage)
                            if (currentPage != null) {
                                DropdownMenuItem(
                                    text = { Text("Edit Page") },
                                    onClick = {
                                        onNavigateToEdit(viewModel.documentId, pagerState.currentPage)
                                        showPageMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export as image") },
                                    onClick = {
                                        viewModel.exportPageAsImage(currentPage)?.let { file ->
                                            shareImageFile(context, file)
                                        }
                                        showPageMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Page") },
                                    onClick = {
                                        viewModel.deletePage(currentPage.id)
                                        showPageMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (uiState.canAddPage) onNavigateToScanner(viewModel.documentId) },
                containerColor = if (uiState.canAddPage)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add page")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Main pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { pageIndex ->
                val page = uiState.pages.getOrNull(pageIndex)
                if (page == null) return@HorizontalPager
                if (viewModel.isPageFileMissing(page)) {
                    MissingPagePlaceholder(modifier = Modifier.fillMaxSize())
                } else {
                    AsyncImage(
                        model = page.imagePath,
                        contentDescription = "Page ${pageIndex + 1}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Page indicator
            if (uiState.pages.isNotEmpty()) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${uiState.pages.size}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(4.dp)
                )
            }

            // Thumbnail strip with drag-to-reorder
            if (uiState.pages.isNotEmpty()) {
                ReorderableThumbnailList(
                    pages = uiState.pages,
                    currentPageIndex = pagerState.currentPage,
                    onReorder = { viewModel.reorderPages(it) },
                    modifier = Modifier.fillMaxWidth().height(80.dp)
                )
            }
        }
    }

    // Export progress overlay
    if (uiState.isExporting) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

private fun shareImageFile(context: android.content.Context, file: java.io.File) {
    val uri = androidx.core.content.FileProvider.getUriForFile(
        context, "${context.packageName}.fileprovider", file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Image"))
}
