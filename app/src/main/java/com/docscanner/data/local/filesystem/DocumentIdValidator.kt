package com.docscanner.data.local.filesystem

import java.io.File

private val UUID_REGEX = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

internal fun requireValidDocumentId(documentId: String) {
    require(documentId.matches(UUID_REGEX)) { "Invalid document ID format" }
}

/**
 * CWE-22 guard: rejects an image path that escapes the app's documents tree.
 * Canonicalization resolves `..` and symlinks (and the /data/data vs /data/user/0
 * alias), so it must run on Dispatchers.IO — all callers already do.
 */
internal fun requireContainedImagePath(imagePath: String, filesDir: File): File {
    val canonical = File(imagePath).canonicalFile
    val root = File(filesDir, "documents").canonicalFile
    require(canonical.path.startsWith(root.path + File.separator)) {
        "Image path outside documents directory"
    }
    return canonical
}
