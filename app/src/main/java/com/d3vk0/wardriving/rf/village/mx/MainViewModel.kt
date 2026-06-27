package com.d3vk0.wardriving.rf.village.mx

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.d3vk0.wardriving.rf.village.mx.core.csv.CsvExportManager
import com.d3vk0.wardriving.rf.village.mx.core.csv.UploadFileKind
import com.d3vk0.wardriving.rf.village.mx.core.domain.ApiConfig
import com.d3vk0.wardriving.rf.village.mx.core.domain.LiveCounters
import com.d3vk0.wardriving.rf.village.mx.core.domain.MapPin
import com.d3vk0.wardriving.rf.village.mx.core.domain.SessionFilter
import com.d3vk0.wardriving.rf.village.mx.core.domain.SessionSettings
import com.d3vk0.wardriving.rf.village.mx.core.local.WardrivingSessionEntity
import com.d3vk0.wardriving.rf.village.mx.core.repository.AuthRepository
import com.d3vk0.wardriving.rf.village.mx.core.repository.UploadAttemptResult
import com.d3vk0.wardriving.rf.village.mx.core.repository.UploadRepository
import com.d3vk0.wardriving.rf.village.mx.core.repository.SessionUploadState
import com.d3vk0.wardriving.rf.village.mx.core.repository.WardrivingRepository
import com.d3vk0.wardriving.rf.village.mx.core.repository.isOnPlatform
import com.d3vk0.wardriving.rf.village.mx.core.repository.toUserFacingMessage
import com.d3vk0.wardriving.rf.village.mx.core.settings.AppSettingsStore
import com.d3vk0.wardriving.rf.village.mx.core.security.AuthTokenStore
import com.d3vk0.wardriving.rf.village.mx.service.WardrivingForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class MainUiState(
    val authenticated: Boolean = false,
    val sessions: List<WardrivingSessionEntity> = emptyList(),
    val activeSessionId: String? = null,
    val isStopping: Boolean = false,
    val counters: LiveCounters = LiveCounters(),
    val liveMapPins: List<MapPin> = emptyList(),
    val sessionDetailMapPins: List<MapPin> = emptyList(),
    val sessionUploadState: SessionUploadState = SessionUploadState("No subido", true),
    val uploadingSessionId: String? = null,
    val settings: SessionSettings = SessionSettings(),
    val status: String = "Idle",
    val lastExportPath: String? = null,
    val sessionFilter: SessionFilter = SessionFilter.ALL,
)

data class UiErrorEvent(val message: String)

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val authRepository: AuthRepository,
    private val wardrivingRepository: WardrivingRepository,
    private val settingsStore: AppSettingsStore,
    private val csvExportManager: CsvExportManager,
    private val uploadRepository: UploadRepository,
    private val apiConfig: ApiConfig,
    private val authTokenStore: AuthTokenStore,
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(MainUiState(authenticated = authRepository.hasToken()))
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    private val _uiErrors = MutableSharedFlow<UiErrorEvent>(extraBufferCapacity = 8)
    val uiErrors: SharedFlow<UiErrorEvent> = _uiErrors.asSharedFlow()
    private var countersJob: Job? = null
    private var countersSessionId: String? = null
    private var liveMapPinsJob: Job? = null
    private var liveMapPinsSessionId: String? = null
    private var sessionDetailMapPinsJob: Job? = null
    private var sessionUploadJob: Job? = null
    private var settingsJob: Job? = null
    private var sessionFilterJob: Job? = null
    private var sessionsJob: Job? = null

    init {
        viewModelScope.launch {
            authTokenStore.invalidations.collect { invalidation ->
                stopAuthenticatedObservers()
                _uiState.update {
                    it.copy(
                        authenticated = false,
                        status = "Sesión API rechazada (HTTP ${invalidation.httpCode})",
                    )
                }
            }
        }
        if (authRepository.hasToken()) {
            startAuthenticatedObservers()
        }
    }

    private fun startAuthenticatedObservers() {
        if (sessionsJob?.isActive == true) return
        settingsJob = viewModelScope.launch {
            settingsStore.settings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
        sessionFilterJob = viewModelScope.launch {
            settingsStore.sessionFilter.collect { filter ->
                _uiState.update { it.copy(sessionFilter = filter) }
            }
        }
        sessionsJob = viewModelScope.launch {
            wardrivingRepository.observeSessions().collect { sessions ->
                val storedActive = sessions.firstOrNull { it.status == "RUNNING" || it.status == "PAUSED" }
                _uiState.update {
                    val visibleActive = storedActive.takeUnless { _ -> it.isStopping }
                    it.copy(
                        sessions = sessions,
                        activeSessionId = visibleActive?.id,
                        isStopping = it.isStopping && storedActive != null,
                    )
                }
                val active = storedActive.takeUnless { _uiState.value.isStopping }
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

    private fun stopAuthenticatedObservers() {
        settingsJob?.cancel()
        sessionFilterJob?.cancel()
        sessionsJob?.cancel()
        countersJob?.cancel()
        liveMapPinsJob?.cancel()
        sessionDetailMapPinsJob?.cancel()
        sessionUploadJob?.cancel()
        settingsJob = null
        sessionFilterJob = null
        sessionsJob = null
        countersSessionId = null
        liveMapPinsSessionId = null
        _uiState.update {
            it.copy(
                sessions = emptyList(),
                activeSessionId = null,
                isStopping = false,
                counters = LiveCounters(),
                liveMapPins = emptyList(),
                sessionDetailMapPins = emptyList(),
                sessionUploadState = SessionUploadState("No subido", true),
                uploadingSessionId = null,
                settings = SessionSettings(),
                sessionFilter = SessionFilter.ALL,
            )
        }
    }

    fun login(identifier: String, password: String) = viewModelScope.launch {
        try {
            authRepository.login(identifier, password)
            applyAuthenticatedDefaults()
            startAuthenticatedObservers()
            _uiState.update { it.copy(authenticated = true, status = "Logged in") }
        } catch (error: Throwable) {
            reportError(error, "Login failed")
        }
    }

    fun register(username: String, email: String, password: String, passwordConfirm: String) = viewModelScope.launch {
        try {
            authRepository.register(username, email, password, passwordConfirm)
            applyAuthenticatedDefaults()
            startAuthenticatedObservers()
            _uiState.update { it.copy(authenticated = true, status = "Registered") }
        } catch (error: Throwable) {
            reportError(error, "Register failed")
        }
    }

    fun recover(identifier: String) = viewModelScope.launch {
        try {
            authRepository.recoverPassword(identifier)
            _uiState.update { it.copy(status = "Recovery request sent") }
        } catch (error: Throwable) {
            reportError(error, "Recovery failed")
        }
    }

    fun logout() {
        authRepository.logout()
        stopAuthenticatedObservers()
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
        if (_uiState.value.isStopping) {
            _uiState.update { it.copy(status = "La finalización ya está en curso") }
            return
        }
        if (_uiState.value.activeSessionId == null) {
            _uiState.update { it.copy(status = "No hay una sesión activa") }
            return
        }
        service(WardrivingForegroundService.ACTION_STOP)
        _uiState.update { it.copy(activeSessionId = null, isStopping = true, status = "Finalizando sesión") }
    }

    fun updateSettings(transform: (SessionSettings) -> SessionSettings) = viewModelScope.launch {
        try {
            settingsStore.update(transform)
        } catch (error: Throwable) {
            reportError(error, "No se pudo guardar la configuración")
        }
    }

    fun applyAuthenticatedDefaults() = viewModelScope.launch {
        if (authRepository.hasToken()) {
            settingsStore.enableUploadByDefaultForAuthenticatedUser()
        }
    }

    fun exportWifiBle(sessionId: String) = export { csvExportManager.exportWifiBle(sessionId) }

    fun exportLte(sessionId: String) = export { csvExportManager.exportLte(sessionId) }

    fun exportZip(sessionId: String) = export { csvExportManager.exportZip(sessionId) }

    fun setSessionFilter(filter: SessionFilter) {
        viewModelScope.launch {
            settingsStore.saveSessionFilter(filter)
        }
    }

    private fun switchToPlatformFilterIfNeeded() {
        if (_uiState.value.sessionFilter != SessionFilter.PROCESSED) {
            viewModelScope.launch {
                settingsStore.saveSessionFilter(SessionFilter.PROCESSED)
            }
        }
    }

    fun uploadSession(sessionId: String) {
        if (_uiState.value.uploadingSessionId != null) {
            viewModelScope.launch { reportError("Ya hay un upload en curso") }
            return
        }
        _uiState.update { it.copy(uploadingSessionId = sessionId, status = "Preparando upload") }
        viewModelScope.launch {
            runCatching {
                val exports = csvExportManager.exportFilesForUpload(sessionId)
                if (exports.isEmpty()) return@runCatching emptyList<UploadAttemptResult>()
                exports.forEach { export ->
                    val type = when (export.kind) {
                        UploadFileKind.WIFI_BLE -> apiConfig.wifiBleUploadType
                        UploadFileKind.LTE -> apiConfig.lteUploadType
                    }
                    uploadRepository.enqueuePending(sessionId, type, export.file, export.sampleCount)
                }
                uploadRepository.uploadSessionPending(sessionId)
            }.onSuccess { results ->
                val failure = results.filterIsInstance<UploadAttemptResult.Failure>().firstOrNull()
                val processing = results.filterIsInstance<UploadAttemptResult.Success>().any { !it.isProcessed }
                if (failure != null) {
                    reportError(failure.message)
                } else if (results.isEmpty()) {
                    reportError("Sin datos disponibles")
                } else {
                    switchToPlatformFilterIfNeeded()
                }
                _uiState.update {
                    it.copy(
                        status = failure?.message
                            ?: when {
                                results.isEmpty() -> "Sin datos disponibles"
                                processing -> "Upload aceptado: Procesando"
                                else -> "Upload procesado"
                            },
                    )
                }
            }.onFailure { error ->
                reportError(error, "Upload failed")
            }
            _uiState.update { it.copy(uploadingSessionId = null) }
        }
    }

    fun saveLastExportAs(destination: android.net.Uri) = viewModelScope.launch {
        val source = _uiState.value.lastExportPath?.let(::File)?.takeIf { it.exists() }
        if (source == null) {
            reportError("No hay una exportación disponible")
            return@launch
        }
        runCatching {
            getApplication<Application>().contentResolver.openOutputStream(destination)?.use { output ->
                source.inputStream().use { input -> input.copyTo(output) }
            } ?: error("Could not open selected destination")
        }.onSuccess {
            _uiState.update { it.copy(status = "Saved ${source.name}") }
        }.onFailure { error ->
            reportError(error, "Save failed")
        }
    }

    fun shareLastExport() {
        val file = _uiState.value.lastExportPath?.let(::File)?.takeIf { it.exists() }
        if (file == null) {
            viewModelScope.launch { reportError("No hay una exportación disponible") }
            return
        }
        runCatching {
            val uri: Uri = FileProvider.getUriForFile(getApplication(), "${BuildConfig.APPLICATION_ID}.files", file)
            val intent = Intent(Intent.ACTION_SEND)
                .setType(if (file.extension == "zip") "application/zip" else "text/csv")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            getApplication<Application>().startActivity(Intent.createChooser(intent, "Share export").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure { error -> viewModelScope.launch { reportError(error, "No se pudo compartir la exportación") } }
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
        if (_uiState.value.sessions.any { it.id == sessionId && it.uploaded }) {
            switchToPlatformFilterIfNeeded()
        }
        if (sessionDetailMapPinsJob?.isActive == true) sessionDetailMapPinsJob?.cancel()
        sessionDetailMapPinsJob = viewModelScope.launch {
            wardrivingRepository.observeLatestMapPins(sessionId).collect { pins ->
                _uiState.update { it.copy(sessionDetailMapPins = pins) }
            }
        }
        sessionUploadJob?.cancel()
        sessionUploadJob = viewModelScope.launch {
            uploadRepository.observeSessionUploadState(sessionId).collect { uploadState ->
                _uiState.update { it.copy(sessionUploadState = uploadState) }
                if (uploadState.isOnPlatform()) {
                    switchToPlatformFilterIfNeeded()
                }
            }
        }
    }

    private fun export(block: suspend () -> File) = viewModelScope.launch {
        runCatching { block() }
            .onSuccess { file -> _uiState.update { it.copy(lastExportPath = file.absolutePath, status = "Exported ${file.name}") } }
            .onFailure { error -> reportError(error, "Export failed") }
    }

    private suspend fun reportError(error: Throwable, fallback: String) {
        reportError(error.toUserFacingMessage(fallback))
    }

    private suspend fun reportError(message: String) {
        _uiState.update { it.copy(status = message) }
        _uiErrors.emit(UiErrorEvent(message))
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

}
