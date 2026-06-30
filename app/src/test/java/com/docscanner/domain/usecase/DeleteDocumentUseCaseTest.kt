package com.docscanner.domain.usecase

import com.docscanner.data.repository.DocumentRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails

class DeleteDocumentUseCaseTest {

    @Test
    fun `invoke calls repository deleteDocument with the given id`() = runBlocking {
        val repo = mock(DocumentRepository::class.java)
        DeleteDocumentUseCase(repo)("doc-abc")
        val calls = mockingDetails(repo).invocations
            .filter { it.method.name == "deleteDocument" }
        assertEquals(1, calls.size)
        assertEquals("doc-abc", calls[0].getArgument<String>(0))
    }

    @Test
    fun `invoke is called exactly once per invocation`() = runBlocking {
        val repo = mock(DocumentRepository::class.java)
        DeleteDocumentUseCase(repo)("doc-1")
        val callCount = mockingDetails(repo).invocations
            .count { it.method.name == "deleteDocument" }
        assertEquals(1, callCount)
    }

    @Test
    fun `invoke propagates exception from repository`() = runBlocking {
        val repo = mock(DocumentRepository::class.java)
        // Stub via doThrow so the stub applies to any call regardless of continuation identity
        org.mockito.Mockito.doThrow(RuntimeException("already deleted"))
            .`when`(repo).deleteDocument(org.mockito.ArgumentMatchers.anyString())
        try {
            DeleteDocumentUseCase(repo)("doc-x")
            org.junit.Assert.fail("Expected RuntimeException")
        } catch (e: RuntimeException) {
            assertEquals("already deleted", e.message)
        }
    }
}
