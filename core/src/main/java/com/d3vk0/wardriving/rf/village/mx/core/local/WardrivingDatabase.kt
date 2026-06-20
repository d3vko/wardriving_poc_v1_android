package com.d3vk0.wardriving.rf.village.mx.core.local

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        WardrivingSessionEntity::class,
        LocationSampleEntity::class,
        WifiBleSampleEntity::class,
        LteSampleEntity::class,
        PendingUploadEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class WardrivingDatabase : RoomDatabase() {
    abstract fun wardrivingDao(): WardrivingDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE lte_samples ADD COLUMN pci INTEGER")
                db.execSQL("ALTER TABLE lte_samples ADD COLUMN earfcn INTEGER")
                db.execSQL("ALTER TABLE pending_uploads ADD COLUMN alreadyUploaded INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE pending_uploads SET alreadyUploaded = 1 WHERE uploadedAt IS NOT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pending_uploads_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionId TEXT NOT NULL,
                        uploadType TEXT NOT NULL,
                        filePath TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        sampleCount INTEGER NOT NULL,
                        retryCount INTEGER NOT NULL,
                        lastError TEXT,
                        remoteId TEXT,
                        remoteSource TEXT,
                        remoteHashSha256 TEXT,
                        isProcessed INTEGER,
                        responseReceivedAt INTEGER,
                        uploadedAt INTEGER
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO pending_uploads_new (
                        id, sessionId, uploadType, filePath, createdAt, sampleCount,
                        retryCount, lastError, uploadedAt
                    )
                    SELECT p.id, p.sessionId, p.uploadType, p.filePath, p.createdAt,
                        p.sampleCount, p.retryCount, p.lastError, p.uploadedAt
                    FROM pending_uploads p
                    INNER JOIN (
                        SELECT sessionId, uploadType, MAX(id) AS keepId
                        FROM pending_uploads GROUP BY sessionId, uploadType
                    ) kept ON kept.keepId = p.id
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE pending_uploads")
                db.execSQL("ALTER TABLE pending_uploads_new RENAME TO pending_uploads")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_pending_uploads_sessionId_uploadType " +
                        "ON pending_uploads (sessionId, uploadType)",
                )
            }
        }
    }
}
