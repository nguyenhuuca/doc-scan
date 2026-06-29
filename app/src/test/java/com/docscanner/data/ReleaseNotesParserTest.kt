package com.docscanner.data

import com.docscanner.ui.settings.parseReleaseNotes
import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseNotesParserTest {

    private val sampleYaml = """
        - version: "1.0.1"
          date: "28/06/2026"
          changes:
            - "Bug fix A"
            - "Bug fix B"

        - version: "1.0.0"
          date: "01/06/2026"
          changes:
            - "First release"
    """.trimIndent()

    @Test
    fun parsesTwoVersions() {
        val notes = parseReleaseNotes(sampleYaml)
        assertEquals(2, notes.size)
    }

    @Test
    fun parsesVersionAndDate() {
        val notes = parseReleaseNotes(sampleYaml)
        assertEquals("1.0.1", notes[0].version)
        assertEquals("28/06/2026", notes[0].date)
        assertEquals("1.0.0", notes[1].version)
        assertEquals("01/06/2026", notes[1].date)
    }

    @Test
    fun parsesChanges() {
        val notes = parseReleaseNotes(sampleYaml)
        assertEquals(listOf("Bug fix A", "Bug fix B"), notes[0].changes)
        assertEquals(listOf("First release"), notes[1].changes)
    }

    @Test
    fun emptyYamlReturnsEmptyList() {
        assertEquals(emptyList<Any>(), parseReleaseNotes(""))
    }

    @Test
    fun handlesQuotedAndUnquotedValues() {
        val yaml = """
            - version: 2.0.0
              date: 01/01/2027
              changes:
                - Unquoted change
        """.trimIndent()
        val notes = parseReleaseNotes(yaml)
        assertEquals("2.0.0", notes[0].version)
        assertEquals("Unquoted change", notes[0].changes[0])
    }
}
