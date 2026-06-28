package com.docscanner.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database migrations. One object per version bump.
 *
 * ── Checklist when changing the schema ───────────────────────────────────────
 *  1. Edit the entity (DocumentEntity / PageEntity)
 *  2. Bump `version` in @Database inside AppDatabase.kt
 *  3. Add a Migration object below (copy the V1_V2 template)
 *  4. Add it to AppDatabase.ALL
 *  5. Build: ./gradlew kspDebugKotlin
 *     → Room writes app/schemas/com.docscanner.data.local.db.AppDatabase/{version}.json
 *  6. Commit the new schema JSON alongside the code change
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * Common SQL patterns:
 *   Add nullable column  : ALTER TABLE t ADD COLUMN col TYPE
 *   Add non-null column  : ALTER TABLE t ADD COLUMN col TYPE NOT NULL DEFAULT value
 *   Create new table     : CREATE TABLE IF NOT EXISTS t (...)
 *   Rename table         : ALTER TABLE old RENAME TO new
 *   Drop column (API 35+): ALTER TABLE t DROP COLUMN col
 *   Drop column (< 35)   : recreate table (copy → drop → rename)
 */
object Migrations {

    // Template — uncomment and fill in when you need version 1 → 2
    //
    // val V1_V2 = object : Migration(1, 2) {
    //     override fun migrate(db: SupportSQLiteDatabase) {
    //         // Example: add an optional 'tags' column to documents
    //         db.execSQL("ALTER TABLE documents ADD COLUMN tags TEXT")
    //     }
    // }
}
