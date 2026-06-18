package com.d3vk0.wardriving.rf.village.mx.core.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "wardriving_sessions")
data class WardrivingSessionEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val endedAt: Long?,
    val status: String,
    val deviceSource: String,
    val notes: String?,
    val wifiEnabled: Boolean,
    val bleEnabled: Boolean,
    val lteEnabled: Boolean,
    val uploaded: Boolean,
    val localExportPath: String?,
)

@Entity(
    tableName = "location_samples",
    indices = [Index("sessionId"), Index(value = ["sessionId", "timestamp"], unique = true)],
)
data class LocationSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double?,
    val accuracyMeters: Float?,
    val speedMetersPerSecond: Float?,
    val bearingDegrees: Float?,
)

@Entity(
    tableName = "wifi_ble_samples",
    indices = [
        Index("sessionId"),
        Index(value = ["sessionId", "type", "mac", "timestamp"], unique = true),
    ],
)
data class WifiBleSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val timestamp: Long,
    val type: String,
    val mac: String,
    val ssid: String,
    val authMode: String,
    val channel: String,
    val rssi: Int?,
    val latitude: Double?,
    val longitude: Double?,
    val altitudeMeters: Double?,
    val accuracyMeters: Float?,
    val rawPayload: String?,
)

@Entity(
    tableName = "lte_samples",
    indices = [
        Index("sessionId"),
        Index(value = ["sessionId", "timestamp", "mcc", "mnc", "lac", "cellId"], unique = true),
    ],
)
data class LteSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val timestamp: Long,
    val technology: String,
    val state: String,
    val mcc: String?,
    val mnc: String?,
    val lac: Int?,
    val cellId: Int?,
    val band: String?,
    val rssi: Int?,
    val rsrp: Int?,
    val rsrq: Int?,
    val sinr: Int?,
    val operator: String?,
    val longitude: Double?,
    val latitude: Double?,
    val rawPayload: String?,
)

@Entity(tableName = "pending_uploads", indices = [Index("sessionId"), Index("uploadType")])
data class PendingUploadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val uploadType: String,
    val filePath: String,
    val createdAt: Long,
    val sampleCount: Int,
    val retryCount: Int,
    val lastError: String?,
    val uploadedAt: Long?,
)
