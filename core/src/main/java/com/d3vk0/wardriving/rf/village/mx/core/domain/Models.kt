package com.d3vk0.wardriving.rf.village.mx.core.domain

data class GeoLocation(
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double?,
    val accuracyMeters: Float?,
    val speedMetersPerSecond: Float?,
    val bearingDegrees: Float?,
)

data class SessionSettings(
    val sampleIntervalMillis: Long = 20_000L,
    val wifiEnabled: Boolean = true,
    val bleEnabled: Boolean = true,
    val lteEnabled: Boolean = true,
    val uploadAfterSession: Boolean = false,
    val localCsvExport: Boolean = true,
    val keepScreenAwake: Boolean = false,
)

enum class SessionStatus { RUNNING, PAUSED, STOPPED }
enum class SampleType { WIFI, BLE }
enum class MapPinType { WIFI, BLE, LTE }

data class MapPin(
    val id: String,
    val sessionId: String,
    val type: MapPinType,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val rssiOrSignal: Int?,
)

data class ApiConfig(
    val baseUrl: String,
    val loginPath: String,
    val registerPath: String,
    val passwordRecoveryPath: String,
    val uploadPath: String,
    val wifiBleUploadType: String,
    val lteUploadType: String,
)

fun ApiConfig.resolvedUploadUrl(): String = resolveRelativeUrl(uploadPath)

fun ApiConfig.resolveRelativeUrl(path: String): String {
    val normalizedBase = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
    return normalizedBase + path.trimStart('/')
}

data class LiveCounters(
    val wifiCount: Int = 0,
    val bleCount: Int = 0,
    val lteCount: Int = 0,
    val gpsStatus: String = "Waiting",
    val uploadStatus: String = "Idle",
)
