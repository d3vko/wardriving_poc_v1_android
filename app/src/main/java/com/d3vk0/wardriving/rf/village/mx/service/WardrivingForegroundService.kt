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
import com.d3vk0.wardriving.rf.village.mx.MainActivity
import com.d3vk0.wardriving.rf.village.mx.R
import com.d3vk0.wardriving.rf.village.mx.core.ble.BleScanner
import com.d3vk0.wardriving.rf.village.mx.core.domain.GeoLocation
import com.d3vk0.wardriving.rf.village.mx.core.domain.SessionSettings
import com.d3vk0.wardriving.rf.village.mx.core.duplicate.DuplicateFilter
import com.d3vk0.wardriving.rf.village.mx.core.location.LocationTracker
import com.d3vk0.wardriving.rf.village.mx.core.repository.WardrivingRepository
import com.d3vk0.wardriving.rf.village.mx.core.scan.ScanCadence
import com.d3vk0.wardriving.rf.village.mx.core.settings.AppSettingsStore
import com.d3vk0.wardriving.rf.village.mx.core.telephony.TelephonyScanner
import com.d3vk0.wardriving.rf.village.mx.core.wifi.WifiScanner
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WardrivingForegroundService : Service() {
    @Inject lateinit var repository: WardrivingRepository
    @Inject lateinit var settingsStore: AppSettingsStore
    @Inject lateinit var locationTracker: LocationTracker
    @Inject lateinit var wifiScanner: WifiScanner
    @Inject lateinit var bleScanner: BleScanner
    @Inject lateinit var telephonyScanner: TelephonyScanner

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var collectionJob: Job? = null
    private var locationJob: Job? = null
    private var stopJob: Job? = null
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
            startLocationCollection(session.id)
            wifiScanner.acquire()
            startBle(session.id, settings)
            while (collectionJob?.isActive == true) {
                if (!paused) collectOnce(session.id, settings)
                delay(ScanCadence.effectiveWifiPeriod(settings.sampleIntervalMillis))
            }
        }
    }

    private fun startLocationCollection(sessionId: String) {
        locationJob?.cancel()
        locationJob = serviceScope.launch {
            runCatching {
                locationTracker.locations(ScanCadence.GPS_PERIOD_MS).collect { location ->
                    latestLocation = location
                    repository.insertLocation(sessionId, location)
                }
            }
        }
    }

    private fun startBle(sessionId: String, settings: SessionSettings) {
        if (!settings.bleEnabled) return
        bleScanner.start { result ->
            if (paused) return@start
            val sample = bleScanner.toEntity(result, sessionId, latestLocation)
            if (sample.mac.isBlank()) return@start
            if (!duplicateFilter.shouldKeep("ble:${sample.mac}", sample.timestamp)) return@start
            serviceScope.launch { repository.insertWifiBle(listOf(sample)) }
        }
    }

    private suspend fun collectOnce(sessionId: String, settings: SessionSettings) {
        val location = latestLocation
        coroutineScope {
            val wifiJob = async {
                if (!settings.wifiEnabled) return@async
                val samples = wifiScanner.scan(sessionId, location)
                    .filter { duplicateFilter.shouldKeep("wifi:${it.mac}", it.timestamp) }
                repository.insertWifiBle(samples)
            }
            val lteJob = async {
                if (!settings.lteEnabled) return@async
                val samples = telephonyScanner.scan(sessionId, location)
                    .filter { duplicateFilter.shouldKeep("lte:${it.mcc}:${it.mnc}:${it.lac}:${it.cellId}", it.timestamp) }
                repository.insertLte(samples)
            }
            wifiJob.await()
            lteJob.await()
        }
    }

    private fun pauseCollection() {
        paused = true
        bleScanner.stop()
        currentSessionId?.let { sessionId ->
            serviceScope.launch { repository.pauseSession(sessionId) }
        }
        startForeground(NOTIFICATION_ID, notification("Wardriving session paused"))
    }

    private fun resumeCollection() {
        paused = false
        currentSessionId?.let { sessionId ->
            serviceScope.launch {
                val settings = settingsStore.settings.first()
                repository.resumeSession(sessionId)
                startBle(sessionId, settings)
            }
        }
        startForeground(NOTIFICATION_ID, notification("Wardriving session active"))
    }

    private fun stopCollection() {
        if (stopJob?.isActive == true) return
        val inMemorySessionId = currentSessionId
        currentSessionId = null
        stopRadios()
        collectionJob?.cancel()
        collectionJob = null
        locationJob?.cancel()
        locationJob = null
        stopJob = serviceScope.launch {
            val sessionId = inMemorySessionId ?: repository.getActiveSession()?.id
            if (sessionId != null) repository.stopSession(sessionId)
            stopSelf()
        }
    }

    private fun stopRadios() {
        bleScanner.stop()
        wifiScanner.release()
    }

    override fun onDestroy() {
        stopRadios()
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
