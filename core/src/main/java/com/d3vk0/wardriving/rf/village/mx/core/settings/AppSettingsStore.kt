package com.d3vk0.wardriving.rf.village.mx.core.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.d3vk0.wardriving.rf.village.mx.core.domain.SessionSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appSettingsDataStore by preferencesDataStore("wardriving_settings")

class AppSettingsStore(private val context: Context) {
    private object Keys {
        val interval = longPreferencesKey("sample_interval_millis")
        val wifi = booleanPreferencesKey("wifi_enabled")
        val ble = booleanPreferencesKey("ble_enabled")
        val lte = booleanPreferencesKey("lte_enabled")
        val upload = booleanPreferencesKey("upload_after_session")
        val export = booleanPreferencesKey("local_csv_export")
        val keepAwake = booleanPreferencesKey("keep_screen_awake")
        val anonymizeSsid = booleanPreferencesKey("anonymize_ssid")
        val anonymizeBleName = booleanPreferencesKey("anonymize_ble_name")
        val uploadDefaultAppliedForJwt = booleanPreferencesKey("upload_default_applied_for_jwt")
    }

    val settings: Flow<SessionSettings> = context.appSettingsDataStore.data.map { prefs ->
        SessionSettings(
            sampleIntervalMillis = prefs[Keys.interval] ?: 20_000L,
            wifiEnabled = prefs[Keys.wifi] ?: true,
            bleEnabled = prefs[Keys.ble] ?: true,
            lteEnabled = prefs[Keys.lte] ?: true,
            uploadAfterSession = prefs[Keys.upload] ?: false,
            localCsvExport = prefs[Keys.export] ?: true,
            keepScreenAwake = prefs[Keys.keepAwake] ?: false,
            anonymizeSsid = prefs[Keys.anonymizeSsid] ?: false,
            anonymizeBleName = prefs[Keys.anonymizeBleName] ?: false,
        )
    }

    suspend fun update(transform: (SessionSettings) -> SessionSettings) {
        context.appSettingsDataStore.edit { prefs ->
            val current = SessionSettings(
                sampleIntervalMillis = prefs[Keys.interval] ?: 20_000L,
                wifiEnabled = prefs[Keys.wifi] ?: true,
                bleEnabled = prefs[Keys.ble] ?: true,
                lteEnabled = prefs[Keys.lte] ?: true,
                uploadAfterSession = prefs[Keys.upload] ?: false,
                localCsvExport = prefs[Keys.export] ?: true,
                keepScreenAwake = prefs[Keys.keepAwake] ?: false,
                anonymizeSsid = prefs[Keys.anonymizeSsid] ?: false,
                anonymizeBleName = prefs[Keys.anonymizeBleName] ?: false,
            )
            val next = transform(current)
            prefs[Keys.interval] = next.sampleIntervalMillis
            prefs[Keys.wifi] = next.wifiEnabled
            prefs[Keys.ble] = next.bleEnabled
            prefs[Keys.lte] = next.lteEnabled
            prefs[Keys.upload] = next.uploadAfterSession
            prefs[Keys.export] = next.localCsvExport
            prefs[Keys.keepAwake] = next.keepScreenAwake
            prefs[Keys.anonymizeSsid] = next.anonymizeSsid
            prefs[Keys.anonymizeBleName] = next.anonymizeBleName
        }
    }

    suspend fun enableUploadByDefaultForAuthenticatedUser() {
        context.appSettingsDataStore.edit { prefs ->
            if (prefs[Keys.uploadDefaultAppliedForJwt] != true) {
                prefs[Keys.upload] = true
                prefs[Keys.uploadDefaultAppliedForJwt] = true
            }
        }
    }
}
