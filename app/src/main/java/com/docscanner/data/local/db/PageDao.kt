package com.docscanner.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.docscanner.data.local.entity.PageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PageDao {

    @Query("SELECT * FROM pages WHERE documentId = :documentId ORDER BY pageNumber ASC")
    fun getPagesByDocumentId(documentId: String): Flow<List<PageEntity>>

    @Query("SELECT * FROM pages WHERE documentId = :documentId ORDER BY pageNumber ASC")
    suspend fun getPagesByDocumentIdSync(documentId: String): List<PageEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPage(page: PageEntity)

    @Update
    suspend fun updatePage(page: PageEntity)

    @Update
    suspend fun updatePages(pages: List<PageEntity>)

    @Query("DELETE FROM pages WHERE id = :pageId")
    suspend fun deletePage(pageId: String)

    @Query("DELETE FROM pages WHERE documentId = :documentId")
    suspend fun deletePagesByDocumentId(documentId: String)

    @Query("SELECT COUNT(*) FROM pages WHERE documentId = :documentId")
    suspend fun getPageCount(documentId: String): Int

    @Query("UPDATE pages SET pageNumber = :pageNumber WHERE id = :pageId")
    suspend fun updatePageNumber(pageId: String, pageNumber: Int)
}
