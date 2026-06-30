package com.docscanner.domain.usecase

import com.docscanner.common.exceptions.DocumentNameException
import com.docscanner.data.repository.DocumentRepository
import com.docscanner.domain.model.Document
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails

class RenameDocumentUseCaseTest {

    private val doc = Document("id", "Original Name", 0L, 0L, 1, null)

    /** Configures getDocumentById to return [doc] for any String argument. */
    private fun stubGetDocumentById(repo: DocumentRepository) = runBlocking {
        doReturn(doc).`when`(repo).getDocumentById(org.mockito.ArgumentMatchers.anyString())
    }

    private fun renameCallsOn(repo: DocumentRepository): List<org.mockito.invocation.Invocation> =
        mockingDetails(repo).invocations.filter { it.method.name == "renameDocument" }

    @Test
    fun `rename with valid name succeeds`() = runBlocking {
        val repo = mock(DocumentRepository::class.java)
        stubGetDocumentById(repo)
        RenameDocumentUseCase(repo)("id", "New Name")
        val calls = renameCallsOn(repo)
        assertEquals(1, calls.size)
        assertEquals("New Name", calls[0].getArgument<String>(1))
    }

    @Test
    fun `rename with same name skips write EC-7`() = runBlocking {
        val repo = mock(DocumentRepository::class.java)
        stubGetDocumentById(repo)
        RenameDocumentUseCase(repo)("id", "Original Name")
        assertEquals(0, renameCallsOn(repo).size)
    }

    @Test
    fun `rename with empty name throws DocumentNameException`() = runBlocking {
        val repo = mock(DocumentRepository::class.java)
        try {
            RenameDocumentUseCase(repo)("id", "   ")
            fail()
        } catch (e: DocumentNameException) { }
    }

    @Test
    fun `rename with 51 chars throws DocumentNameException`() = runBlocking {
        val repo = mock(DocumentRepository::class.java)
        try {
            RenameDocumentUseCase(repo)("id", "a".repeat(51))
            fail()
        } catch (e: DocumentNameException) { }
    }

    @Test
    fun `rename with exactly 50 chars succeeds`() = runBlocking {
        val repo = mock(DocumentRepository::class.java)
        stubGetDocumentById(repo)
        RenameDocumentUseCase(repo)("id", "a".repeat(50))
        val calls = renameCallsOn(repo)
        assertEquals(1, calls.size)
        assertEquals("a".repeat(50), calls[0].getArgument<String>(1))
    }

    @Test
    fun `rename with forbidden char throws DocumentNameException`() = runBlocking {
        val repo = mock(DocumentRepository::class.java)
        val forbidden = listOf("/", "\\", ":", "*", "?", "\"", "<", ">", "|")
        forbidden.forEach { char ->
            try {
                RenameDocumentUseCase(repo)("id", "name${char}test")
                fail("Expected exception for char: $char")
            } catch (e: DocumentNameException) { }
        }
    }

    @Test
    fun `rename strips leading and trailing whitespace`() = runBlocking {
        val repo = mock(DocumentRepository::class.java)
        stubGetDocumentById(repo)
        RenameDocumentUseCase(repo)("id", "  New Name  ")
        val calls = renameCallsOn(repo)
        assertEquals(1, calls.size)
        assertEquals("New Name", calls[0].getArgument<String>(1))
    }

    @Test
    fun `rename when document not found still calls renameDocument`() = runBlocking {
        val repo = mock(DocumentRepository::class.java)
        // getDocumentById returns null by default for a Mockito mock with nullable return type
        RenameDocumentUseCase(repo)("missing-id", "New Name")
        val calls = renameCallsOn(repo)
        assertEquals(1, calls.size)
        assertEquals("missing-id", calls[0].getArgument<String>(0))
        assertEquals("New Name", calls[0].getArgument<String>(1))
    }
}
