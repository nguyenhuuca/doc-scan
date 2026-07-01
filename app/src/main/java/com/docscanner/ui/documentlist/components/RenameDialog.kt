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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.docscanner.R

@Composable
fun RenameDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentName) }
    var error by remember { mutableStateOf<String?>(null) }

    val forbiddenChars = Regex("""[/\\:*?"<>|]""")
    val emptyError = stringResource(R.string.name_empty_error)
    val tooLongError = stringResource(R.string.name_too_long_error)
    val invalidCharsError = stringResource(R.string.name_invalid_chars_error)

    fun validate(value: String): String? {
        val trimmed = value.trim()
        return when {
            trimmed.isEmpty() -> emptyError
            trimmed.length > 50 -> tooLongError
            forbiddenChars.containsMatchIn(trimmed) -> invalidCharsError
            else -> null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_document)) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        error = validate(it)
                    },
                    label = { Text(stringResource(R.string.document_name_label)) },
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
            ) { Text(stringResource(R.string.rename)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
