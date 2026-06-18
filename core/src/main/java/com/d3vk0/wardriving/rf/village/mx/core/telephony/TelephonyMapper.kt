package com.d3vk0.wardriving.rf.village.mx.core.telephony

import android.os.Build
import android.telephony.CellIdentityLte
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import com.d3vk0.wardriving.rf.village.mx.core.domain.GeoLocation
import com.d3vk0.wardriving.rf.village.mx.core.local.LteSampleEntity

class TelephonyMapper(private val bandMapper: LteBandMapper = LteBandMapper()) {
    fun mapLte(
        sessionId: String,
        cellInfo: CellInfoLte,
        operatorName: String?,
        location: GeoLocation?,
    ): LteSampleEntity {
        val identity = cellInfo.cellIdentity
        val signal = cellInfo.cellSignalStrength
        return LteSampleEntity(
            sessionId = sessionId,
            timestamp = cellInfo.timeStampMillisOrNow(),
            technology = "LTE",
            state = cellInfo.connectionStateForCsv(),
            mcc = identity.mccForCsv(),
            mnc = identity.mncForCsv(),
            lac = identity.tac.unavailableToNull(),
            cellId = identity.ci.unavailableToNull(),
            pci = identity.pci.unavailableToNull(),
            earfcn = identity.earfcnUnavailableToNull(),
            band = bandMapper.bandFromEarfcn(identity.earfcnUnavailableToNull()),
            rssi = signal.dbm.unavailableToNull(),
            rsrp = signal.rsrp.unavailableToNull(),
            rsrq = signal.rsrq.unavailableToNull(),
            sinr = signal.rssnr.unavailableToNull(),
            operator = operatorName.orEmpty(),
            longitude = location?.longitude,
            latitude = location?.latitude,
            rawPayload = "earfcn=${identity.earfcnUnavailableToNull()};timingAdvance=${signal.timingAdvance.unavailableToNull()}",
        )
    }

    private fun CellInfo.timeStampMillisOrNow(): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            timestampMillis.takeIf { it > 0L } ?: System.currentTimeMillis()
        } else {
            System.currentTimeMillis()
        }
    }

    private fun CellInfo.connectionStateForCsv(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            cellConnectionStatus.toString()
        } else {
            if (isRegistered) "1" else "0"
        }
    }

    private fun CellIdentityLte.mccForCsv(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) mccString else mcc.unavailableToNull()?.toString()
    }

    private fun CellIdentityLte.mncForCsv(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) mncString else mnc.unavailableToNull()?.toString()
    }

    private fun CellIdentityLte.earfcnUnavailableToNull(): Int? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) earfcn.unavailableToNull() else null
    }

    private fun Int.unavailableToNull(): Int? {
        return if (this == CellInfo.UNAVAILABLE || this == Int.MAX_VALUE || this == Int.MIN_VALUE) null else this
    }
}
