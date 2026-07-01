package com.docscanner.data.local.filesystem

import org.junit.Assert.fail
import org.junit.Test

class DocumentIdValidatorTest {

    private fun assertValid(id: String) {
        requireValidDocumentId(id)
    }

    private fun assertInvalid(id: String) {
        try {
            requireValidDocumentId(id)
            fail("Expected IllegalArgumentException for input: '$id'")
        } catch (e: IllegalArgumentException) { /* pass */ }
    }

    // ── Valid UUIDs ───────────────────────────────────────────────────────────

    @Test
    fun `valid lowercase UUID passes`() {
        assertValid("550e8400-e29b-41d4-a716-446655440000")
    }

    @Test
    fun `valid uppercase UUID passes`() {
        assertValid("550E8400-E29B-41D4-A716-446655440000")
    }

    @Test
    fun `valid mixed-case UUID passes`() {
        assertValid("550e8400-E29B-41d4-A716-446655440000")
    }

    @Test
    fun `all-zeros UUID passes`() {
        assertValid("00000000-0000-0000-0000-000000000000")
    }

    @Test
    fun `all-f UUID passes`() {
        assertValid("ffffffff-ffff-ffff-ffff-ffffffffffff")
    }

    // ── Invalid UUIDs ─────────────────────────────────────────────────────────

    @Test
    fun `empty string is rejected`() {
        assertInvalid("")
    }

    @Test
    fun `UUID without hyphens is rejected`() {
        assertInvalid("550e8400e29b41d4a716446655440000")
    }

    @Test
    fun `UUID with hyphens in wrong positions is rejected`() {
        assertInvalid("550e840-0e29b-41d4-a716-446655440000")
    }

    @Test
    fun `too short last segment is rejected`() {
        assertInvalid("550e8400-e29b-41d4-a716-44665544000")
    }

    @Test
    fun `too long last segment is rejected`() {
        assertInvalid("550e8400-e29b-41d4-a716-4466554400000")
    }

    @Test
    fun `invalid hex character g is rejected`() {
        assertInvalid("550e8400-e29b-41d4-a716-44665544000g")
    }

    @Test
    fun `UUID with extra prefix is rejected`() {
        assertInvalid("x550e8400-e29b-41d4-a716-446655440000")
    }

    @Test
    fun `UUID with extra suffix is rejected`() {
        assertInvalid("550e8400-e29b-41d4-a716-446655440000x")
    }

    @Test
    fun `plain text is rejected`() {
        assertInvalid("not-a-uuid-at-all")
    }

    @Test
    fun `path traversal attempt is rejected`() {
        assertInvalid("../../../etc/passwd-000-0000-0000-000000000000")
    }

    @Test
    fun `UUID with spaces is rejected`() {
        assertInvalid("550e8400-e29b-41d4-a716 446655440000")
    }
}
