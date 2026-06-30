package com.docscanner.data.local.filesystem

private val UUID_REGEX = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

internal fun requireValidDocumentId(documentId: String) {
    require(documentId.matches(UUID_REGEX)) { "Invalid document ID format" }
}
