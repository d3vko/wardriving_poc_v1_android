package com.d3vk0.wardriving.rf.village.mx.core.repository

import com.d3vk0.wardriving.rf.village.mx.core.domain.GeoLocation
import com.d3vk0.wardriving.rf.village.mx.core.domain.LiveCounters
import com.d3vk0.wardriving.rf.village.mx.core.domain.MapPin
import com.d3vk0.wardriving.rf.village.mx.core.domain.MapPinType
import com.d3vk0.wardriving.rf.village.mx.core.domain.SessionSettings
import com.d3vk0.wardriving.rf.village.mx.core.domain.SessionStatus
import com.d3vk0.wardriving.rf.village.mx.core.local.LocationSampleEntity
import com.d3vk0.wardriving.rf.village.mx.core.local.LteSampleEntity
import com.d3vk0.wardriving.rf.village.mx.core.local.WardrivingDao
import com.d3vk0.wardriving.rf.village.mx.core.local.WardrivingSessionEntity
import com.d3vk0.wardriving.rf.village.mx.core.local.WifiBleSampleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID

class WardrivingRepository(private val dao: WardrivingDao) {
    private val mapPinLimit = 100

    fun observeSessions(): Flow<List<WardrivingSessionEntity>> = dao.observeSessions()

    suspend fun getActiveSession(): WardrivingSessionEntity? = dao.getActiveSession()

    suspend fun startSession(settings: SessionSettings, deviceSource: String): WardrivingSessionEntity {
        val session = WardrivingSessionEntity(
            id = UUID.randomUUID().toString(),
            startedAt = System.currentTimeMillis(),
            endedAt = null,
            status = SessionStatus.RUNNING.name,
            deviceSource = deviceSource,
            notes = null,
            wifiEnabled = settings.wifiEnabled,
            bleEnabled = settings.bleEnabled,
            lteEnabled = settings.lteEnabled,
            uploaded = false,
            localExportPath = null,
        )
        dao.upsertSession(session)
        return session
    }

    suspend fun pauseSession(sessionId: String) {
        dao.updateSessionStatus(sessionId, SessionStatus.PAUSED.name, null)
    }

    suspend fun resumeSession(sessionId: String) {
        dao.updateSessionStatus(sessionId, SessionStatus.RUNNING.name, null)
    }

    suspend fun stopSession(sessionId: String) {
        dao.updateSessionStatus(sessionId, SessionStatus.STOPPED.name, System.currentTimeMillis())
    }

    suspend fun markSessionExported(sessionId: String, localExportPath: String, uploaded: Boolean = false) {
        dao.updateSessionExport(sessionId, uploaded, localExportPath)
    }

    suspend fun insertLocation(sessionId: String, location: GeoLocation) {
        dao.insertLocation(
            LocationSampleEntity(
                sessionId = sessionId,
                timestamp = location.timestamp,
                latitude = location.latitude,
                longitude = location.longitude,
                altitudeMeters = location.altitudeMeters,
                accuracyMeters = location.accuracyMeters,
                speedMetersPerSecond = location.speedMetersPerSecond,
                bearingDegrees = location.bearingDegrees,
            ),
        )
    }

    suspend fun insertWifiBle(samples: List<WifiBleSampleEntity>) = dao.insertWifiBle(samples)

    suspend fun insertLte(samples: List<LteSampleEntity>) = dao.insertLte(samples)

    fun observeCounters(sessionId: String): Flow<LiveCounters> {
        return combine(
            dao.observeWifiCount(sessionId),
            dao.observeBleCount(sessionId),
            dao.observeLteCount(sessionId),
            dao.observeLatestLocation(sessionId),
        ) { wifi, ble, lte, location ->
            LiveCounters(
                wifiCount = wifi,
                bleCount = ble,
                lteCount = lte,
                gpsStatus = if (location == null) "Waiting" else "Locked ${location.latitude}, ${location.longitude}",
            )
        }
    }

    fun observeLatestMapPins(sessionId: String?): Flow<List<MapPin>> {
        return combine(
            dao.observeLatestGeolocatedWifiBleSamples(sessionId, mapPinLimit),
            dao.observeLatestGeolocatedLteSamples(sessionId, mapPinLimit),
        ) { wifiBle, lte ->
            combineMapPins(wifiBle, lte, mapPinLimit)
        }
    }

    suspend fun deleteAllLocalData() {
        dao.deleteAllPendingUploads()
        dao.deleteAllLte()
        dao.deleteAllWifiBle()
        dao.deleteAllLocations()
        dao.deleteAllSessions()
    }
}

fun combineMapPins(
    wifiBle: List<WifiBleSampleEntity>,
    lte: List<LteSampleEntity>,
    limit: Int = 100,
): List<MapPin> {
    return (wifiBle.mapNotNull { it.toMapPin() } + lte.mapNotNull { it.toMapPin() })
        .sortedByDescending { it.timestamp }
        .take(limit)
}

private fun WifiBleSampleEntity.toMapPin(): MapPin? {
    val lat = latitude ?: return null
    val lon = longitude ?: return null
    val pinType = when (type.uppercase()) {
        "WIFI" -> MapPinType.WIFI
        "BLE" -> MapPinType.BLE
        else -> return null
    }
    val fallback = if (pinType == MapPinType.WIFI) mac else "BLE $mac"
    return MapPin(
        id = "wifi_ble:$id",
        sessionId = sessionId,
        type = pinType,
        label = ssid.takeIf { it.isNotBlank() } ?: fallback,
        latitude = lat,
        longitude = lon,
        timestamp = timestamp,
        rssiOrSignal = rssi,
    )
}

private fun LteSampleEntity.toMapPin(): MapPin? {
    val lat = latitude ?: return null
    val lon = longitude ?: return null
    val cellLabel = listOfNotNull(operator, technology, cellId?.let { "Cell $it" })
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { "LTE sample" }
    return MapPin(
        id = "lte:$id",
        sessionId = sessionId,
        type = MapPinType.LTE,
        label = cellLabel,
        latitude = lat,
        longitude = lon,
        timestamp = timestamp,
        rssiOrSignal = rsrp ?: rssi,
    )
}
