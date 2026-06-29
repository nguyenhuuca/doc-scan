package com.docscanner.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.docscanner.data.local.entity.DocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents ORDER BY updatedAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDocument(document: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :documentId")
    suspend fun deleteDocument(documentId: String)

    @Query("SELECT COUNT(*) FROM documents")
    suspend fun getDocumentCount(): Int

    @Query("SELECT * FROM documents WHERE id = :documentId")
    suspend fun getDocumentById(documentId: String): DocumentEntity?

    @Query("UPDATE documents SET name = :name, updatedAt = :updatedAt WHERE id = :documentId")
    suspend fun renameDocument(documentId: String, name: String, updatedAt: Long)

    @Query("UPDATE documents SET pageCount = :pageCount, updatedAt = :updatedAt, thumbnailPath = :thumbnailPath WHERE id = :documentId")
    suspend fun updateDocumentMeta(documentId: String, pageCount: Int, updatedAt: Long, thumbnailPath: String?)
}
