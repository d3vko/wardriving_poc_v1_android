package com.d3vk0.wardriving.rf.village.mx.core.csv

import com.d3vk0.wardriving.rf.village.mx.core.local.WifiBleSampleEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class WigleCsvFormatter {
    companion object {
        const val HEADER =
            "MAC,SSID,AuthMode,FirstSeen,Channel,RSSI,CurrentLatitude,CurrentLongitude,AltitudeMeters,AccuracyMeters,Type"
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }

    fun format(samples: List<WifiBleSampleEntity>, sessionStartedAt: Long = 0L): String {
        return buildString {
            appendLine(HEADER)
            samples.forEach { sample ->
                val ts = if (sample.timestamp > sessionStartedAt) sample.timestamp else sessionStartedAt
                val (lat, lon) = CsvExportCoords.forWifiBle(sample)
                appendLine(
                    listOf(
                        sample.mac,
                        sample.ssid,
                        sample.authMode,
                        dateFormat.format(Date(ts)),
                        sample.channel,
                        sample.rssi,
                        lat,
                        lon,
                        CsvDecimalFormatter.measurement(sample.altitudeMeters),
                        CsvDecimalFormatter.measurement(sample.accuracyMeters),
                        sample.type,
                    ).joinToString(",") { CsvEscaper.escape(it) },
                )
            }
        }
    }
}
