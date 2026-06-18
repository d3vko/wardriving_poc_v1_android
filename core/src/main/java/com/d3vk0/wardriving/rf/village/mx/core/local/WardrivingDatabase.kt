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
    version = 2,
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
    }
}
