package com.d3vk0.wardriving.rf.village.mx.core.location

import android.annotation.SuppressLint
import android.os.Looper
import com.d3vk0.wardriving.rf.village.mx.core.domain.GeoLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LocationTracker(private val fusedLocationProviderClient: FusedLocationProviderClient) {
    @SuppressLint("MissingPermission")
    fun locations(intervalMillis: Long): Flow<GeoLocation> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
            .setMinUpdateIntervalMillis((intervalMillis / 2).coerceAtLeast(1_000L))
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { location ->
                    trySend(
                        GeoLocation(
                            timestamp = location.time,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            altitudeMeters = if (location.hasAltitude()) location.altitude else null,
                            accuracyMeters = if (location.hasAccuracy()) location.accuracy else null,
                            speedMetersPerSecond = if (location.hasSpeed()) location.speed else null,
                            bearingDegrees = if (location.hasBearing()) location.bearing else null,
                        ),
                    )
                }
            }
        }
        fusedLocationProviderClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { fusedLocationProviderClient.removeLocationUpdates(callback) }
    }
}
