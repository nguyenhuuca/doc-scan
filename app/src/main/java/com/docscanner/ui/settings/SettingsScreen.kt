package com.docscanner.ui.settings

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.docscanner.BuildConfig
import com.docscanner.common.AppLanguage
import com.docscanner.MyApplication
import com.docscanner.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as MyApplication

    val documentCount = remember {
        runBlocking { app.container.documentRepository.getDocumentCount() }
    }

    val storageUsedBytes = remember {
        calculateStorageUsed(File(context.filesDir, "documents"))
    }

    var releaseNotes by remember { mutableStateOf<List<ReleaseNote>>(emptyList()) }
    LaunchedEffect(Unit) {
        releaseNotes = withContext(Dispatchers.IO) { loadReleaseNotes(context) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            item {
                // Uninstall warning banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.uninstall_warning),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                SettingsRow(label = stringResource(R.string.app_version_label), value = BuildConfig.VERSION_NAME)
                SettingsRow(label = stringResource(R.string.documents_label), value = "$documentCount / 100")
                SettingsRow(label = stringResource(R.string.storage_used_label), value = formatBytes(storageUsedBytes))
                Spacer(modifier = Modifier.height(24.dp))
                LanguagePicker()
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.whats_new_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            items(releaseNotes) { note ->
                ReleaseNoteCard(note)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

private val LANGUAGE_OPTIONS = listOf(
    null to R.string.language_system_default,
    "en" to R.string.language_english,
    "vi" to R.string.language_vietnamese
)

@Composable
private fun LanguagePicker() {
    val context = LocalContext.current
    val currentTag = AppLanguage.getTag(context)

    fun applyLanguage(tag: String?) {
        AppLanguage.setTag(context, tag)
        // API 33+: the OS recreates activities on its own. API < 33: no such mechanism exists,
        // so trigger it manually for the new Configuration (set via attachBaseContext) to apply.
        if (Build.VERSION.SDK_INT < 33) {
            (context as? Activity)?.recreate()
        }
    }

    Column {
        Text(
            text = stringResource(R.string.language_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LANGUAGE_OPTIONS.forEach { (tag, labelRes) ->
            val selected = currentTag == tag
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = selected, onClick = { applyLanguage(tag) }),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selected, onClick = { applyLanguage(tag) })
                Text(stringResource(labelRes), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun SettingsRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun calculateStorageUsed(dir: File): Long {
    if (!dir.exists()) return 0L
    return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024 * 1024 -> String.format(Locale.US, "%.1f GB", bytes / (1024.0 * 1024 * 1024))
    bytes >= 1024L * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024))
    bytes >= 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}

@Composable
private fun ReleaseNoteCard(note: ReleaseNote) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row {
                Text(
                    text = "v${note.version}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = note.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            note.changes.forEach { change ->
                Text(
                    text = "• $change",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}
