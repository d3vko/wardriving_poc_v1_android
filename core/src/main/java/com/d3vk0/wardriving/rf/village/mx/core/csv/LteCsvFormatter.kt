package com.d3vk0.wardriving.rf.village.mx.core.csv

import com.d3vk0.wardriving.rf.village.mx.core.local.LteSampleEntity
import com.d3vk0.wardriving.rf.village.mx.core.telephony.LteBandMapper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class LteCsvFormatter(private val bandMapper: LteBandMapper = LteBandMapper()) {
    companion object {
        const val HEADER =
            "Timestamp,Tecnología,TipoCelda,Estado,MCC,MNC,LAC,CellID,eNodeB,Sector,PCI,Banda,EARFCN,FreqDL_MHz,FreqUL_MHz,RSSI,RSRP,RSRQ,SINR,Operador,Longitud,Latitud"
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun format(samples: List<LteSampleEntity>): String {
        return buildString {
            appendLine(HEADER)
            samples.forEach { sample ->
                val cellId = sample.cellId
                val earfcn = sample.earfcn
                appendLine(
                    listOf(
                        dateFormat.format(Date(sample.timestamp)),
                        sample.technology,
                        "LTE",
                        sample.state,
                        sample.mcc,
                        sample.mnc,
                        sample.lac,
                        cellId,
                        cellId?.let { it / 256 },
                        cellId?.let { it % 256 },
                        sample.pci,
                        sample.band ?: bandMapper.bandFromEarfcn(earfcn),
                        earfcn,
                        CsvDecimalFormatter.mhz(bandMapper.downlinkMhzFromEarfcn(earfcn)),
                        CsvDecimalFormatter.mhz(bandMapper.uplinkMhzFromEarfcn(earfcn)),
                        sample.rssi,
                        sample.rsrp,
                        sample.rsrq,
                        sample.sinr,
                        sample.operator,
                        CsvDecimalFormatter.longitude(sample.longitude),
                        CsvDecimalFormatter.latitude(sample.latitude),
                    ).joinToString(",") { CsvEscaper.escape(it) },
                )
            }
        }
    }
}
