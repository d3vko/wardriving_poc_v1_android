package com.d3vk0.wardriving.rf.village.mx.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.d3vk0.wardriving.rf.village.mx.MainActivity
import com.d3vk0.wardriving.rf.village.mx.R
import com.d3vk0.wardriving.rf.village.mx.core.ble.BleScanner
import com.d3vk0.wardriving.rf.village.mx.core.csv.CsvExportManager
import com.d3vk0.wardriving.rf.village.mx.core.domain.ApiConfig
import com.d3vk0.wardriving.rf.village.mx.core.domain.GeoLocation
import com.d3vk0.wardriving.rf.village.mx.core.domain.SessionSettings
import com.d3vk0.wardriving.rf.village.mx.core.duplicate.DuplicateFilter
import com.d3vk0.wardriving.rf.village.mx.core.location.LocationTracker
import com.d3vk0.wardriving.rf.village.mx.core.repository.UploadRepository
import com.d3vk0.wardriving.rf.village.mx.core.repository.WardrivingRepository
import com.d3vk0.wardriving.rf.village.mx.core.settings.AppSettingsStore
import com.d3vk0.wardriving.rf.village.mx.core.telephony.TelephonyScanner
import com.d3vk0.wardriving.rf.village.mx.core.wifi.WifiScanner
import com.d3vk0.wardriving.rf.village.mx.worker.UploadWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class WardrivingForegroundService : Service() {
    @Inject lateinit var repository: WardrivingRepository
    @Inject lateinit var settingsStore: AppSettingsStore
    @Inject lateinit var locationTracker: LocationTracker
    @Inject lateinit var wifiScanner: WifiScanner
    @Inject lateinit var bleScanner: BleScanner
    @Inject lateinit var telephonyScanner: TelephonyScanner
    @Inject lateinit var csvExportManager: CsvExportManager
    @Inject lateinit var uploadRepository: UploadRepository
    @Inject lateinit var apiConfig: ApiConfig

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var collectionJob: Job? = null
    private var locationJob: Job? = null
    private var currentSessionId: String? = null
    private var latestLocation: GeoLocation? = null
    private var paused = false
    private val duplicateFilter = DuplicateFilter()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action ?: ACTION_START) {
            ACTION_START -> startCollection()
            ACTION_PAUSE -> pauseCollection()
            ACTION_RESUME -> resumeCollection()
            ACTION_STOP -> stopCollection()
        }
        return START_STICKY
    }

    private fun startCollection() {
        if (collectionJob?.isActive == true) return
        startForeground(NOTIFICATION_ID, notification("Starting wardriving session"))
        collectionJob = serviceScope.launch {
            val settings = settingsStore.settings.first()
            val active = repository.getActiveSession()
            val session = active ?: repository.startSession(settings, "android-phone")
            currentSessionId = session.id
            paused = false
            startLocationCollection(session.id, settings)
            while (collectionJob?.isActive == true) {
                if (!paused) collectOnce(session.id, settings)
                delay(settings.sampleIntervalMillis)
            }
        }
    }

    private fun startLocationCollection(sessionId: String, settings: SessionSettings) {
        locationJob?.cancel()
        locationJob = serviceScope.launch {
            runCatching {
                locationTracker.locations(settings.sampleIntervalMillis).collect { location ->
                    latestLocation = location
                    repository.insertLocation(sessionId, location)
                }
            }
        }
    }

    private suspend fun collectOnce(sessionId: String, settings: SessionSettings) {
        val location = latestLocation
        if (settings.wifiEnabled) {
            val samples = wifiScanner.scan(sessionId, location, settings.anonymizeSsid)
                .filter { duplicateFilter.shouldKeep("wifi:${it.mac}", it.timestamp) }
            repository.insertWifiBle(samples)
        }
        if (settings.bleEnabled) {
            val samples = bleScanner.scan(sessionId, location, settings.anonymizeBleName)
                .filter { duplicateFilter.shouldKeep("ble:${it.mac}", it.timestamp) }
            repository.insertWifiBle(samples)
        }
        if (settings.lteEnabled) {
            val samples = telephonyScanner.scan(sessionId, location)
                .filter { duplicateFilter.shouldKeep("lte:${it.mcc}:${it.mnc}:${it.lac}:${it.cellId}", it.timestamp) }
            repository.insertLte(samples)
        }
    }

    private fun pauseCollection() {
        paused = true
        currentSessionId?.let { sessionId ->
            serviceScope.launch { repository.pauseSession(sessionId) }
        }
        startForeground(NOTIFICATION_ID, notification("Wardriving session paused"))
    }

    private fun resumeCollection() {
        paused = false
        currentSessionId?.let { sessionId ->
            serviceScope.launch { repository.resumeSession(sessionId) }
        }
        startForeground(NOTIFICATION_ID, notification("Wardriving session active"))
    }

    private fun stopCollection() {
        val sessionId = currentSessionId
        collectionJob?.cancel()
        locationJob?.cancel()
        if (sessionId == null) {
            stopSelf()
            return
        }
        serviceScope.launch {
            val settings = settingsStore.settings.first()
            repository.stopSession(sessionId)
            if (settings.localCsvExport || settings.uploadAfterSession) {
                val wifiBle = csvExportManager.exportWifiBle(sessionId)
                val lte = csvExportManager.exportLte(sessionId)
                repository.markSessionExported(sessionId, "${wifiBle.absolutePath};${lte.absolutePath}")
                if (settings.uploadAfterSession) {
                    uploadRepository.enqueuePending(sessionId, apiConfig.wifiBleUploadType, wifiBle, sampleCount(wifiBle))
                    uploadRepository.enqueuePending(sessionId, apiConfig.lteUploadType, lte, sampleCount(lte))
                    WorkManager.getInstance(applicationContext)
                        .enqueue(OneTimeWorkRequestBuilder<UploadWorker>().build())
                }
            }
            stopSelf()
        }
    }

    private fun sampleCount(file: File): Int = file.readLines().drop(1).size

    override fun onDestroy() {
        collectionJob?.cancel()
        locationJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun notification(text: String): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentTitle(getString(R.string.wardriving_notification_title))
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(openApp)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.wardriving_notification_channel),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
    }

    companion object {
        const val ACTION_START = "com.d3vk0.wardriving.action.START"
        const val ACTION_PAUSE = "com.d3vk0.wardriving.action.PAUSE"
        const val ACTION_RESUME = "com.d3vk0.wardriving.action.RESUME"
        const val ACTION_STOP = "com.d3vk0.wardriving.action.STOP"
        private const val CHANNEL_ID = "wardriving_collection"
        private const val NOTIFICATION_ID = 33420

        fun command(context: Context, action: String): Intent =
            Intent(context, WardrivingForegroundService::class.java).setAction(action)
    }
}
