package com.d3vk0.wardriving.rf.village.mx.core.csv

import com.d3vk0.wardriving.rf.village.mx.core.local.LteSampleEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class LteCsvFormatter {
    companion object {
        const val HEADER =
            "Timestamp,Technology,State,MCC,MNC,LAC,CellID,Band,RSSI,RSRP,RSRQ,SINR,Operator,Longitude,Latitude"
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun format(samples: List<LteSampleEntity>): String {
        return buildString {
            appendLine(HEADER)
            samples.forEach { sample ->
                appendLine(
                    listOf(
                        dateFormat.format(Date(sample.timestamp)),
                        sample.technology,
                        sample.state,
                        sample.mcc,
                        sample.mnc,
                        sample.lac,
                        sample.cellId,
                        sample.band,
                        sample.rssi,
                        sample.rsrp,
                        sample.rsrq,
                        sample.sinr,
                        sample.operator,
                        sample.longitude,
                        sample.latitude,
                    ).joinToString(",") { CsvEscaper.escape(it) },
                )
            }
        }
    }
}
