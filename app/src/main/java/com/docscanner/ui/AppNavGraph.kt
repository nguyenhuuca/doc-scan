package com.docscanner.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.docscanner.MyApplication
import com.docscanner.ui.documentlist.DocumentListScreen
import com.docscanner.ui.documentlist.DocumentListViewModel
import com.docscanner.ui.edit.EditScreen
import com.docscanner.ui.edit.EditViewModel
import com.docscanner.ui.scanner.ScannerScreen
import com.docscanner.ui.scanner.ScannerViewModel
import com.docscanner.ui.settings.SettingsScreen
import com.docscanner.ui.settings.SettingsViewModel
import com.docscanner.ui.settings.loadReleaseNotes
import com.docscanner.ui.viewer.DocumentViewerScreen
import com.docscanner.ui.viewer.DocumentViewerViewModel

object Routes {
    const val DOCUMENT_LIST = "document_list"
    const val SCANNER = "scanner/{documentId}"
    const val EDIT = "edit/{documentId}/{pageIndex}"
    const val VIEWER = "viewer/{documentId}"
    const val SETTINGS = "settings"

    fun scanner(documentId: String = "new") = "scanner/$documentId"
    fun edit(documentId: String, pageIndex: Int) = "edit/$documentId/$pageIndex"
    fun viewer(documentId: String) = "viewer/$documentId"
}

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.DOCUMENT_LIST,
        modifier = modifier
    ) {
        composable(Routes.DOCUMENT_LIST) {
            val viewModel = viewModel<DocumentListViewModel>(
                factory = viewModelFactory {
                    initializer {
                        val app = this[APPLICATION_KEY] as MyApplication
                        DocumentListViewModel(
                            app.container.getDocumentsUseCase,
                            app.container.deleteDocumentUseCase,
                            app.container.renameDocumentUseCase,
                            app.container.exportPdfUseCase
                        )
                    }
                }
            )
            DocumentListScreen(
                viewModel = viewModel,
                onNavigateToScanner = { documentId -> navController.navigate(Routes.scanner(documentId)) },
                onNavigateToViewer = { documentId -> navController.navigate(Routes.viewer(documentId)) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(
            route = Routes.SCANNER,
            arguments = listOf(navArgument("documentId") { type = NavType.StringType })
        ) {
            val viewModel = viewModel<ScannerViewModel>(
                factory = viewModelFactory {
                    initializer {
                        val app = this[APPLICATION_KEY] as MyApplication
                        ScannerViewModel(
                            app.container.saveDocumentUseCase,
                            createSavedStateHandle()
                        )
                    }
                }
            )
            ScannerScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onScanComplete = { documentId ->
                    navController.navigate(Routes.viewer(documentId)) {
                        popUpTo(Routes.DOCUMENT_LIST)
                    }
                }
            )
        }

        composable(
            route = Routes.EDIT,
            arguments = listOf(
                navArgument("documentId") { type = NavType.StringType },
                navArgument("pageIndex") { type = NavType.IntType }
            )
        ) {
            val viewModel = viewModel<EditViewModel>(
                factory = viewModelFactory {
                    initializer {
                        val app = this[APPLICATION_KEY] as MyApplication
                        EditViewModel(
                            app.container.documentRepository,
                            createSavedStateHandle()
                        )
                    }
                }
            )
            EditScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.VIEWER,
            arguments = listOf(navArgument("documentId") { type = NavType.StringType })
        ) {
            val viewModel = viewModel<DocumentViewerViewModel>(
                factory = viewModelFactory {
                    initializer {
                        val app = this[APPLICATION_KEY] as MyApplication
                        DocumentViewerViewModel(
                            app.container.documentRepository,
                            app.container.deleteDocumentUseCase,
                            app.container.exportPdfUseCase,
                            app.container.saveDocumentUseCase,
                            createSavedStateHandle()
                        )
                    }
                }
            )
            DocumentViewerScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToScanner = { documentId -> navController.navigate(Routes.scanner(documentId)) },
                onNavigateToEdit = { documentId, pageIndex -> navController.navigate(Routes.edit(documentId, pageIndex)) }
            )
        }

        composable(Routes.SETTINGS) {
            val viewModel = viewModel<SettingsViewModel>(
                factory = viewModelFactory {
                    initializer {
                        val app = this[APPLICATION_KEY] as MyApplication
                        SettingsViewModel(
                            app.container.documentRepository,
                            app.container.imageStorage
                        ) { loadReleaseNotes(app) }
                    }
                }
            )
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
