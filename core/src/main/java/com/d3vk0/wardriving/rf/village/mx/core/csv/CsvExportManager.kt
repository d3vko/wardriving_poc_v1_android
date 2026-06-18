package com.d3vk0.wardriving.rf.village.mx.core.csv

import android.content.Context
import com.d3vk0.wardriving.rf.village.mx.core.local.WardrivingDao
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class CsvExportManager(
    private val context: Context,
    private val dao: WardrivingDao,
    private val wigleCsvFormatter: WigleCsvFormatter = WigleCsvFormatter(),
    private val lteCsvFormatter: LteCsvFormatter = LteCsvFormatter(),
) {
    private val fileStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    suspend fun exportWifiBle(sessionId: String): File {
        val samples = dao.getWifiBleSamples(sessionId)
        val file = File(exportDir(), "wifi_ble_${sessionId}_${fileStamp.format(Date())}.csv")
        file.writeText(wigleCsvFormatter.format(samples))
        return file
    }

    suspend fun exportLte(sessionId: String): File {
        val samples = dao.getLteSamples(sessionId)
        val file = File(exportDir(), "lte_${sessionId}_${fileStamp.format(Date())}.csv")
        file.writeText(lteCsvFormatter.format(samples))
        return file
    }

    suspend fun exportZip(sessionId: String): File {
        val wifiBle = exportWifiBle(sessionId)
        val lte = exportLte(sessionId)
        val zip = File(exportDir(), "wardriving_${sessionId}_${fileStamp.format(Date())}.zip")
        ZipOutputStream(zip.outputStream()).use { output ->
            listOf(wifiBle, lte).forEach { file ->
                output.putNextEntry(ZipEntry(file.name))
                file.inputStream().use { it.copyTo(output) }
                output.closeEntry()
            }
        }
        return zip
    }

    private fun exportDir(): File {
        return File(context.getExternalFilesDir(null) ?: context.filesDir, "exports").apply {
            mkdirs()
        }
    }
}
