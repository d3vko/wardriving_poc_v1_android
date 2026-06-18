package com.d3vk0.wardriving.rf.village.mx.core.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        WardrivingSessionEntity::class,
        LocationSampleEntity::class,
        WifiBleSampleEntity::class,
        LteSampleEntity::class,
        PendingUploadEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class WardrivingDatabase : RoomDatabase() {
    abstract fun wardrivingDao(): WardrivingDao
}
