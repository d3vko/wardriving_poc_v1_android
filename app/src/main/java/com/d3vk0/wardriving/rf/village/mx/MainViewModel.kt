package com.d3vk0.wardriving.rf.village.mx

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.d3vk0.wardriving.rf.village.mx.core.csv.CsvExportManager
import com.d3vk0.wardriving.rf.village.mx.core.domain.ApiConfig
import com.d3vk0.wardriving.rf.village.mx.core.domain.LiveCounters
import com.d3vk0.wardriving.rf.village.mx.core.domain.MapPin
import com.d3vk0.wardriving.rf.village.mx.core.domain.SessionSettings
import com.d3vk0.wardriving.rf.village.mx.core.local.WardrivingSessionEntity
import com.d3vk0.wardriving.rf.village.mx.core.repository.AuthRepository
import com.d3vk0.wardriving.rf.village.mx.core.repository.UploadRepository
import com.d3vk0.wardriving.rf.village.mx.core.repository.WardrivingRepository
import com.d3vk0.wardriving.rf.village.mx.core.settings.AppSettingsStore
import com.d3vk0.wardriving.rf.village.mx.service.WardrivingForegroundService
import com.d3vk0.wardriving.rf.village.mx.worker.UploadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class MainUiState(
    val authenticated: Boolean = false,
    val sessions: List<WardrivingSessionEntity> = emptyList(),
    val activeSessionId: String? = null,
    val counters: LiveCounters = LiveCounters(),
    val liveMapPins: List<MapPin> = emptyList(),
    val sessionDetailMapPins: List<MapPin> = emptyList(),
    val settings: SessionSettings = SessionSettings(),
    val status: String = "Idle",
    val lastExportPath: String? = null,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val authRepository: AuthRepository,
    private val wardrivingRepository: WardrivingRepository,
    private val settingsStore: AppSettingsStore,
    private val csvExportManager: CsvExportManager,
    private val uploadRepository: UploadRepository,
    private val apiConfig: ApiConfig,
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(MainUiState(authenticated = authRepository.hasToken()))
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    private var countersJob: Job? = null
    private var countersSessionId: String? = null
    private var liveMapPinsJob: Job? = null
    private var liveMapPinsSessionId: String? = null
    private var sessionDetailMapPinsJob: Job? = null

    init {
        applyAuthenticatedDefaults()
        viewModelScope.launch {
            settingsStore.settings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch {
            wardrivingRepository.observeSessions().collect { sessions ->
                val active = sessions.firstOrNull { it.status == "RUNNING" || it.status == "PAUSED" }
                _uiState.update { it.copy(sessions = sessions, activeSessionId = active?.id) }
                active?.id?.let {
                    observeCounters(it)
                    observeLiveMapPins(it)
                } ?: run {
                    countersJob?.cancel()
                    liveMapPinsJob?.cancel()
                    countersSessionId = null
                    liveMapPinsSessionId = null
                    _uiState.update { it.copy(counters = LiveCounters(), liveMapPins = emptyList()) }
                }
            }
        }
    }

    fun login(identifier: String, password: String) = viewModelScope.launch {
        runCatching { authRepository.login(identifier, password) }
            .onSuccess {
                applyAuthenticatedDefaults()
                _uiState.update { it.copy(authenticated = true, status = "Logged in") }
            }
            .onFailure { error -> _uiState.update { it.copy(status = error.message ?: "Login failed") } }
    }

    fun register(identifier: String, password: String) = viewModelScope.launch {
        runCatching { authRepository.register(identifier, password) }
            .onSuccess {
                applyAuthenticatedDefaults()
                _uiState.update { it.copy(authenticated = true, status = "Registered") }
            }
            .onFailure { error -> _uiState.update { it.copy(status = error.message ?: "Register failed") } }
    }

    fun recover(identifier: String) = viewModelScope.launch {
        runCatching { authRepository.recoverPassword(identifier) }
            .onSuccess { _uiState.update { it.copy(status = "Recovery request sent") } }
            .onFailure { error -> _uiState.update { it.copy(status = error.message ?: "Recovery failed") } }
    }

    fun logout() {
        authRepository.logout()
        _uiState.update { it.copy(authenticated = false, status = "Logged out") }
    }

    fun startSession() {
        service(WardrivingForegroundService.ACTION_START)
        _uiState.update { it.copy(status = "Starting session") }
    }

    fun pauseSession() {
        service(WardrivingForegroundService.ACTION_PAUSE)
        _uiState.update { it.copy(status = "Pause requested") }
    }

    fun resumeSession() {
        service(WardrivingForegroundService.ACTION_RESUME)
        _uiState.update { it.copy(status = "Resume requested") }
    }

    fun stopSession() {
        service(WardrivingForegroundService.ACTION_STOP)
        _uiState.update { it.copy(status = "Stop requested") }
    }

    fun updateSettings(transform: (SessionSettings) -> SessionSettings) = viewModelScope.launch {
        settingsStore.update(transform)
    }

    fun applyAuthenticatedDefaults() = viewModelScope.launch {
        if (authRepository.hasToken()) {
            settingsStore.enableUploadByDefaultForAuthenticatedUser()
        }
    }

    fun exportWifiBle() = export { csvExportManager.exportWifiBle(requireActiveSession()) }

    fun exportLte() = export { csvExportManager.exportLte(requireActiveSession()) }

    fun exportZip() = export { csvExportManager.exportZip(requireActiveSession()) }

    fun uploadExports() = viewModelScope.launch {
        val sessionId = requireActiveSession()
        val wifiBle = csvExportManager.exportWifiBle(sessionId)
        val lte = csvExportManager.exportLte(sessionId)
        uploadRepository.enqueuePending(sessionId, apiConfig.wifiBleUploadType, wifiBle, sampleCount(wifiBle))
        uploadRepository.enqueuePending(sessionId, apiConfig.lteUploadType, lte, sampleCount(lte))
        WorkManager.getInstance(getApplication()).enqueue(OneTimeWorkRequestBuilder<UploadWorker>().build())
        _uiState.update { it.copy(status = "Upload queued") }
    }

    fun saveLastExportAs(destination: android.net.Uri) = viewModelScope.launch {
        val source = _uiState.value.lastExportPath?.let(::File)?.takeIf { it.exists() } ?: return@launch
        runCatching {
            getApplication<Application>().contentResolver.openOutputStream(destination)?.use { output ->
                source.inputStream().use { input -> input.copyTo(output) }
            } ?: error("Could not open selected destination")
        }.onSuccess {
            _uiState.update { it.copy(status = "Saved ${source.name}") }
        }.onFailure { error ->
            _uiState.update { it.copy(status = error.message ?: "Save failed") }
        }
    }

    fun shareLastExport() {
        val file = _uiState.value.lastExportPath?.let(::File)?.takeIf { it.exists() } ?: return
        val uri: Uri = FileProvider.getUriForFile(getApplication(), "${BuildConfig.APPLICATION_ID}.files", file)
        val intent = Intent(Intent.ACTION_SEND)
            .setType(if (file.extension == "zip") "application/zip" else "text/csv")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(Intent.createChooser(intent, "Share export").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun deleteAllLocalData() = viewModelScope.launch {
        wardrivingRepository.deleteAllLocalData()
        _uiState.update {
            it.copy(
                sessions = emptyList(),
                activeSessionId = null,
                counters = LiveCounters(),
                liveMapPins = emptyList(),
                sessionDetailMapPins = emptyList(),
                lastExportPath = null,
                status = "Local data deleted",
            )
        }
    }

    fun observeSessionDetailMapPins(sessionId: String) {
        if (sessionDetailMapPinsJob?.isActive == true) sessionDetailMapPinsJob?.cancel()
        sessionDetailMapPinsJob = viewModelScope.launch {
            wardrivingRepository.observeLatestMapPins(sessionId).collect { pins ->
                _uiState.update { it.copy(sessionDetailMapPins = pins) }
            }
        }
    }

    private fun export(block: suspend () -> File) = viewModelScope.launch {
        runCatching { block() }
            .onSuccess { file -> _uiState.update { it.copy(lastExportPath = file.absolutePath, status = "Exported ${file.name}") } }
            .onFailure { error -> _uiState.update { it.copy(status = error.message ?: "Export failed") } }
    }

    private fun service(action: String) {
        val context = getApplication<Application>()
        val intent = WardrivingForegroundService.command(context, action)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
    }

    private fun observeCounters(sessionId: String) {
        if (countersJob?.isActive == true && countersSessionId == sessionId) return
        countersJob?.cancel()
        countersSessionId = sessionId
        countersJob = viewModelScope.launch {
            wardrivingRepository.observeCounters(sessionId).collect { counters ->
                _uiState.update { it.copy(counters = counters) }
            }
        }
    }

    private fun observeLiveMapPins(sessionId: String) {
        if (liveMapPinsJob?.isActive == true && liveMapPinsSessionId == sessionId) return
        liveMapPinsJob?.cancel()
        liveMapPinsSessionId = sessionId
        liveMapPinsJob = viewModelScope.launch {
            wardrivingRepository.observeLatestMapPins(sessionId).collect { pins ->
                _uiState.update { it.copy(liveMapPins = pins) }
            }
        }
    }

    private suspend fun requireActiveSession(): String {
        return _uiState.value.activeSessionId
            ?: wardrivingRepository.getActiveSession()?.id
            ?: wardrivingRepository.observeSessions().first().firstOrNull()?.id
            ?: error("No session available")
    }

    private fun sampleCount(file: File): Int = file.readLines().drop(1).size
}
