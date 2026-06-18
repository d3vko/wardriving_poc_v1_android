package com.d3vk0.wardriving.rf.village.mx.core.telephony

import android.annotation.SuppressLint
import android.os.Build
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.TelephonyManager
import com.d3vk0.wardriving.rf.village.mx.core.domain.GeoLocation
import com.d3vk0.wardriving.rf.village.mx.core.local.LteSampleEntity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import java.util.concurrent.Executor

class TelephonyScanner(
    private val telephonyManager: TelephonyManager,
    private val mapper: TelephonyMapper = TelephonyMapper(),
) {
    @SuppressLint("MissingPermission")
    suspend fun scan(sessionId: String, location: GeoLocation?): List<LteSampleEntity> {
        val direct = runCatching { telephonyManager.allCellInfo.orEmpty() }.getOrDefault(emptyList())
        val cellInfo = if (direct.isNotEmpty()) direct else requestCellInfoUpdate()
        return cellInfo.filterIsInstance<CellInfoLte>().map {
            mapper.mapLte(sessionId, it, telephonyManager.networkOperatorName, location)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestCellInfoUpdate(): List<CellInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return emptyList()
        return suspendCancellableCoroutine { continuation ->
            val callback = object : TelephonyManager.CellInfoCallback() {
                override fun onCellInfo(cellInfo: MutableList<CellInfo>) {
                    if (continuation.isActive) continuation.resume(cellInfo)
                }

                override fun onError(errorCode: Int, detail: Throwable?) {
                    if (continuation.isActive) continuation.resume(emptyList())
                }
            }
            runCatching { telephonyManager.requestCellInfoUpdate(Executor { it.run() }, callback) }
                .onFailure {
                    if (continuation.isActive) continuation.resume(emptyList())
                }
        }
    }
}
