package com.docscanner.domain.usecase

import com.docscanner.data.repository.DocumentRepository
import com.docscanner.domain.model.Document
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class GetDocumentsUseCaseTest {

    @Test
    fun `returns flow from repository`() = runBlocking {
        val expected = listOf(Document("id1", "Doc 1", 0L, 0L, 1, null))
        val repo = mock(DocumentRepository::class.java)
        `when`(repo.getAllDocuments()).thenReturn(flowOf(expected))
        val result = GetDocumentsUseCase(repo)().first()
        assertEquals(expected, result)
    }

    @Test
    fun `returns empty flow when repository has no documents`() = runBlocking {
        val repo = mock(DocumentRepository::class.java)
        `when`(repo.getAllDocuments()).thenReturn(flowOf(emptyList()))
        val result = GetDocumentsUseCase(repo)().first()
        assertEquals(emptyList<Document>(), result)
    }
}
