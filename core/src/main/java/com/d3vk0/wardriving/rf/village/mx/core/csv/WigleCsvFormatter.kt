package com.d3vk0.wardriving.rf.village.mx.core.csv

import com.d3vk0.wardriving.rf.village.mx.core.local.WifiBleSampleEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class WigleCsvFormatter {
    companion object {
        /*
         * Wigle has adjusted accepted CSV variants over time. Keep the header isolated here
         * so a future Wigle format change is a one-line update plus formatter tests.
         */
        const val HEADER =
            "MAC,SSID,AuthMode,FirstSeen,Channel,RSSI,CurrentLatitude,CurrentLongitude,AltitudeMeters,AccuracyMeters,Type"
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun format(samples: List<WifiBleSampleEntity>): String {
        return buildString {
            appendLine(HEADER)
            samples.forEach { sample ->
                appendLine(
                    listOf(
                        sample.mac,
                        sample.ssid,
                        sample.authMode,
                        dateFormat.format(Date(sample.timestamp)),
                        sample.channel,
                        sample.rssi,
                        CsvDecimalFormatter.latitude(sample.latitude),
                        CsvDecimalFormatter.longitude(sample.longitude),
                        CsvDecimalFormatter.measurement(sample.altitudeMeters),
                        CsvDecimalFormatter.measurement(sample.accuracyMeters),
                        sample.type,
                    ).joinToString(",") { CsvEscaper.escape(it) },
                )
            }
        }
    }
}
