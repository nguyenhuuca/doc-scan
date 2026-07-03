package com.docscanner.domain.usecase

import com.docscanner.common.AppConfig

enum class NameValidationError { EMPTY, TOO_LONG, INVALID_CHARS }

/**
 * Single source of truth for document-name rules, shared by
 * [RenameDocumentUseCase] (throws) and the rename dialog (shows inline error).
 */
object DocumentNameValidator {

    val FORBIDDEN_CHARS = Regex("""[/\\:*?"<>|]""")

    /** Validates the trimmed name; returns null when valid. */
    fun validate(name: String): NameValidationError? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> NameValidationError.EMPTY
            trimmed.length > AppConfig.MAX_DOCUMENT_NAME_LENGTH -> NameValidationError.TOO_LONG
            FORBIDDEN_CHARS.containsMatchIn(trimmed) -> NameValidationError.INVALID_CHARS
            else -> null
        }
    }
}
