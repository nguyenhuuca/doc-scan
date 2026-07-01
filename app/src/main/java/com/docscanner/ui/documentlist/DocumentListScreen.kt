package com.docscanner.ui.documentlist

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
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
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.docscanner.R
import com.docscanner.common.AppConfig
import com.docscanner.domain.model.Document
import com.docscanner.ui.documentlist.components.DeleteConfirmDialog
import com.docscanner.ui.documentlist.components.DocumentCard
import com.docscanner.ui.documentlist.components.RenameDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentListScreen(
    viewModel: DocumentListViewModel,
    onNavigateToScanner: (documentId: String) -> Unit,
    onNavigateToViewer: (documentId: String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var documentForContextMenu by remember { mutableStateOf<Document?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedDocument by remember { mutableStateOf<Document?>(null) }

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
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_pdf)))
            viewModel.clearExportedFile()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.documentLimitReached) {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text(stringResource(R.string.document_limit_reached)) } },
                    state = rememberTooltipState()
                ) {
                    FloatingActionButton(onClick = {}, containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                }
            } else {
                FloatingActionButton(onClick = { onNavigateToScanner("new") }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.scan_new_document))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search + sort bar — only when more than 10 documents
            if (uiState.totalDocumentCount > AppConfig.SEARCH_BAR_MIN_DOCUMENTS) {
                SearchAndSortBar(
                    query = uiState.searchQuery,
                    sortOrder = uiState.sortOrder,
                    onQueryChange = viewModel::setSearchQuery,
                    onSortToggle = viewModel::toggleSortOrder,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            when {
                uiState.totalDocumentCount == 0 && !uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.no_documents_yet), style = MaterialTheme.typography.titleLarge)
                            Text(
                                stringResource(R.string.tap_to_scan_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                uiState.documents.isEmpty() && uiState.totalDocumentCount > 0 -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.no_results_for_query, uiState.searchQuery),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.documents, key = { it.id }) { document ->
                            Box {
                                DocumentCard(
                                    document = document,
                                    onClick = { onNavigateToViewer(document.id) },
                                    onLongClick = {
                                        selectedDocument = document
                                        documentForContextMenu = document
                                    }
                                )
                                if (documentForContextMenu?.id == document.id) {
                                    DropdownMenu(
                                        expanded = true,
                                        onDismissRequest = { documentForContextMenu = null }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.rename)) },
                                            onClick = {
                                                showRenameDialog = true
                                                documentForContextMenu = null
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.export_pdf)) },
                                            onClick = {
                                                selectedDocument?.let { viewModel.exportPdf(it.id) }
                                                documentForContextMenu = null
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.delete)) },
                                            onClick = {
                                                showDeleteDialog = true
                                                documentForContextMenu = null
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        selectedDocument?.let { doc ->
            RenameDialog(
                currentName = doc.name,
                onConfirm = { newName ->
                    viewModel.renameDocument(doc.id, newName)
                    showRenameDialog = false
                },
                onDismiss = { showRenameDialog = false }
            )
        }
    }

    if (showDeleteDialog) {
        selectedDocument?.let { doc ->
            DeleteConfirmDialog(
                onConfirm = {
                    viewModel.deleteDocument(doc.id)
                    showDeleteDialog = false
                },
                onDismiss = { showDeleteDialog = false }
            )
        }
    }
}

@Composable
private fun SearchAndSortBar(
    query: String,
    sortOrder: SortOrder,
    onQueryChange: (String) -> Unit,
    onSortToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(stringResource(R.string.search_documents_placeholder)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear_search))
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = onSortToggle) {
            Icon(
                imageVector = if (sortOrder == SortOrder.DATE_DESC) Icons.Default.Schedule else Icons.Default.SortByAlpha,
                contentDescription = stringResource(if (sortOrder == SortOrder.DATE_DESC) R.string.sort_by_name else R.string.sort_by_date)
            )
        }
    }
}
