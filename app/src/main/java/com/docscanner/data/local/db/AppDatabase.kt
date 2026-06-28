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

    companion object {
        /**
         * Add new migrations here when bumping [version] above.
         *
         * How to add a migration (example: version 1 → 2):
         *
         *   1. Change version = 2 in @Database above
         *   2. Add the migration object to Migrations.kt
         *   3. Add it to this array:
         *        val ALL = arrayOf(Migrations.V1_V2)
         *
         * Run `./gradlew kspDebugKotlin` after changing the schema —
         * Room will write a new JSON file to app/schemas/. Commit it.
         *
         * For simple additions (nullable column, new table) prefer autoMigrations:
         *   @Database(autoMigrations = [AutoMigration(from = 1, to = 2)])
         */
        val ALL = arrayOf<androidx.room.migration.Migration>()
    }
}
