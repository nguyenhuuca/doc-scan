package com.docscanner.ui.settings

import android.content.Context

data class ReleaseNote(
    val version: String,
    val date: String,
    val changes: List<String>
)

fun loadReleaseNotes(context: Context): List<ReleaseNote> {
    val yaml = context.assets.open("release_notes.yaml").bufferedReader().readText()
    return parseReleaseNotes(yaml)
}

internal fun parseReleaseNotes(yaml: String): List<ReleaseNote> {
    val notes = mutableListOf<ReleaseNote>()
    var version = ""
    var date = ""
    val changes = mutableListOf<String>()
    var inChanges = false

    fun flush() {
        if (version.isNotEmpty()) {
            notes.add(ReleaseNote(version, date, changes.toList()))
            changes.clear()
            version = ""
            date = ""
            inChanges = false
        }
    }

    for (rawLine in yaml.lines()) {
        val trimmed = rawLine.trimStart()
        when {
            trimmed.startsWith("- version:") -> {
                flush()
                version = trimmed.substringAfter("version:").trim().trim('"')
            }
            trimmed.startsWith("date:") -> {
                date = trimmed.substringAfter("date:").trim().trim('"')
            }
            trimmed.startsWith("changes:") -> {
                inChanges = true
            }
            inChanges && trimmed.startsWith("- ") -> {
                changes.add(trimmed.removePrefix("- ").trim().trim('"'))
            }
        }
    }
    flush()
    return notes
}
