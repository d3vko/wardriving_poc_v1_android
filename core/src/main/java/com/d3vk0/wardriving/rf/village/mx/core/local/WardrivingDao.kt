package com.d3vk0.wardriving.rf.village.mx.core.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WardrivingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: WardrivingSessionEntity)

    @Query("UPDATE wardriving_sessions SET status = :status, endedAt = :endedAt WHERE id = :sessionId")
    suspend fun updateSessionStatus(sessionId: String, status: String, endedAt: Long?)

    @Query("UPDATE wardriving_sessions SET uploaded = :uploaded, localExportPath = :localExportPath WHERE id = :sessionId")
    suspend fun updateSessionExport(sessionId: String, uploaded: Boolean, localExportPath: String?)

    @Query("SELECT * FROM wardriving_sessions ORDER BY startedAt DESC")
    fun observeSessions(): Flow<List<WardrivingSessionEntity>>

    @Query("SELECT * FROM wardriving_sessions WHERE id = :sessionId")
    suspend fun getSession(sessionId: String): WardrivingSessionEntity?

    @Query("SELECT * FROM wardriving_sessions WHERE status IN ('RUNNING', 'PAUSED') ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveSession(): WardrivingSessionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLocation(sample: LocationSampleEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWifiBle(samples: List<WifiBleSampleEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLte(samples: List<LteSampleEntity>)

    @Query("SELECT * FROM wifi_ble_samples WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getWifiBleSamples(sessionId: String): List<WifiBleSampleEntity>

    @Query("SELECT * FROM lte_samples WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getLteSamples(sessionId: String): List<LteSampleEntity>

    @Query(
        """
        SELECT * FROM wifi_ble_samples
        WHERE latitude IS NOT NULL
            AND longitude IS NOT NULL
            AND (:sessionId IS NULL OR sessionId = :sessionId)
        ORDER BY timestamp DESC
        LIMIT :limit
        """,
    )
    fun observeLatestGeolocatedWifiBleSamples(sessionId: String?, limit: Int): Flow<List<WifiBleSampleEntity>>

    @Query(
        """
        SELECT * FROM lte_samples
        WHERE latitude IS NOT NULL
            AND longitude IS NOT NULL
            AND (:sessionId IS NULL OR sessionId = :sessionId)
        ORDER BY timestamp DESC
        LIMIT :limit
        """,
    )
    fun observeLatestGeolocatedLteSamples(sessionId: String?, limit: Int): Flow<List<LteSampleEntity>>

    @Query("SELECT COUNT(*) FROM wifi_ble_samples WHERE sessionId = :sessionId AND type = 'WIFI'")
    fun observeWifiCount(sessionId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM wifi_ble_samples WHERE sessionId = :sessionId AND type = 'BLE'")
    fun observeBleCount(sessionId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM lte_samples WHERE sessionId = :sessionId")
    fun observeLteCount(sessionId: String): Flow<Int>

    @Query("SELECT * FROM location_samples WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT 1")
    fun observeLatestLocation(sessionId: String): Flow<LocationSampleEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPendingUpload(upload: PendingUploadEntity): Long

    @Query("SELECT * FROM pending_uploads WHERE uploadedAt IS NULL ORDER BY createdAt ASC")
    suspend fun getPendingUploads(): List<PendingUploadEntity>

    @Query("UPDATE pending_uploads SET retryCount = retryCount + 1, lastError = :error WHERE id = :id")
    suspend fun markUploadFailed(id: Long, error: String)

    @Query("UPDATE pending_uploads SET uploadedAt = :uploadedAt, lastError = NULL WHERE id = :id")
    suspend fun markUploadSucceeded(id: Long, uploadedAt: Long)

    @Query("DELETE FROM wardriving_sessions")
    suspend fun deleteAllSessions()

    @Query("DELETE FROM location_samples")
    suspend fun deleteAllLocations()

    @Query("DELETE FROM wifi_ble_samples")
    suspend fun deleteAllWifiBle()

    @Query("DELETE FROM lte_samples")
    suspend fun deleteAllLte()

    @Query("DELETE FROM pending_uploads")
    suspend fun deleteAllPendingUploads()
}
