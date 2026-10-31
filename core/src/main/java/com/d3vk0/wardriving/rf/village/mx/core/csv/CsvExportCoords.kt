package com.d3vk0.wardriving.rf.village.mx.core.csv

import com.d3vk0.wardriving.rf.village.mx.core.local.LteSampleEntity
import com.d3vk0.wardriving.rf.village.mx.core.local.WifiBleSampleEntity

/** GPS inválido o RF no factible → `0.0000000` para que la plataforma ignore la fila. */
object CsvExportCoords {
    const val ZERO_LAT = "0.0000000"
    const val ZERO_LON = "0.0000000"
    const val LAC_UNAVAILABLE_SENTINEL = 65535
    const val CELL_ID_UNAVAILABLE_SENTINEL = 268435455

    fun isValidGps(latitude: Double?, longitude: Double?): Boolean {
        if (latitude == null || longitude == null) return false
        return latitude.isFinite() && longitude.isFinite()
    }

    fun isZeroMccMnc(value: String?): Boolean {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isEmpty()) return true
        return trimmed.toIntOrNull() == 0
    }

    fun isFeasibleWifiBle(sample: WifiBleSampleEntity): Boolean = sample.mac.trim().isNotEmpty()

    fun isFeasibleLte(sample: LteSampleEntity): Boolean {
        if (isZeroMccMnc(sample.mcc) || isZeroMccMnc(sample.mnc)) return false
        val cellId = sample.cellId ?: return false
        if (cellId == CELL_ID_UNAVAILABLE_SENTINEL) return false
        val lac = sample.lac ?: return false
        return lac != LAC_UNAVAILABLE_SENTINEL
    }

    fun forWifiBle(sample: WifiBleSampleEntity): Pair<String, String> {
        if (!isFeasibleWifiBle(sample) || !isValidGps(sample.latitude, sample.longitude)) {
            return ZERO_LAT to ZERO_LON
        }
        return CsvDecimalFormatter.latitude(sample.latitude) to CsvDecimalFormatter.longitude(sample.longitude)
    }

    fun forLte(sample: LteSampleEntity): Pair<String, String> {
        if (!isFeasibleLte(sample) || !isValidGps(sample.latitude, sample.longitude)) {
            return ZERO_LAT to ZERO_LON
        }
        return CsvDecimalFormatter.latitude(sample.latitude) to CsvDecimalFormatter.longitude(sample.longitude)
    }
}
