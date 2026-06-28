package com.docscanner.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.docscanner.data.local.entity.DocumentEntity
import com.docscanner.data.local.entity.PageEntity

@Database(
    entities = [DocumentEntity::class, PageEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun pageDao(): PageDao
}
