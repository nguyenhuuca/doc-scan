package com.docscanner.ui.documentlist.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RenameDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentName) }
    var error by remember { mutableStateOf<String?>(null) }

    val forbiddenChars = Regex("""[/\\:*?"<>|]""")

    fun validate(value: String): String? {
        val trimmed = value.trim()
        return when {
            trimmed.isEmpty() -> "Name cannot be empty."
            trimmed.length > 50 -> "Name must be 50 characters or fewer."
            forbiddenChars.containsMatchIn(trimmed) -> "Name contains invalid characters."
            else -> null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Document") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        error = validate(it)
                    },
                    label = { Text("Document name") },
                    isError = error != null,
                    supportingText = error?.let { msg -> { Text(msg) } },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = text.trim()
                    val validationError = validate(trimmed)
                    if (validationError != null) {
                        error = validationError
                    } else {
                        onConfirm(trimmed)
                    }
                }
            ) { Text("Rename") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
