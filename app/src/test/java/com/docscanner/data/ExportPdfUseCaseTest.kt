package com.docscanner.data

import com.docscanner.common.exceptions.ExportException
import com.docscanner.data.repository.DocumentRepository
import com.docscanner.domain.usecase.ExportPdfUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File
import java.io.IOException

class ExportPdfUseCaseTest {

    @Test
    fun `returns file from repository on success`() = runBlocking {
        val expected = File("/cache/export_123.pdf")
        val repo = mock(DocumentRepository::class.java)
        `when`(repo.exportPdf("doc-id")).thenReturn(expected)
        val result = ExportPdfUseCase(repo)("doc-id")
        assertEquals(expected, result)
    }

    @Test
    fun `wraps IOException as ExportException`() = runBlocking {
        val repo = mock(DocumentRepository::class.java)
        // thenThrow(CheckedException) is rejected by Mockito for suspend functions (no throws clause
        // in compiled bytecode). thenAnswer with a throwing lambda bypasses that validation.
        `when`(repo.exportPdf("doc-id")).thenAnswer { throw IOException("disk full") }
        try {
            ExportPdfUseCase(repo)("doc-id")
            fail("Expected ExportException")
        } catch (e: ExportException) { /* pass */ }
    }

    @Test
    fun `passes documentId to repository`() = runBlocking {
        val repo = mock(DocumentRepository::class.java)
        val outFile = File("/out.pdf")
        `when`(repo.exportPdf("my-doc-123")).thenReturn(outFile)
        val result = ExportPdfUseCase(repo)("my-doc-123")
        assertEquals(outFile, result)
    }
}
