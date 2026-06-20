package com.d3vk0.wardriving.rf.village.mx.core.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class AuthInvalidation(val httpCode: Int)

interface AuthTokenStorage {
    val invalidations: SharedFlow<AuthInvalidation>
    fun getToken(): String?
    fun saveToken(token: String)
    fun clear()
    fun invalidate(httpCode: Int)
}

class AuthTokenStore(context: Context) : AuthTokenStorage {
    private val _invalidations = MutableSharedFlow<AuthInvalidation>(extraBufferCapacity = 1)
    override val invalidations: SharedFlow<AuthInvalidation> = _invalidations.asSharedFlow()

    private val preferences = EncryptedSharedPreferences.create(
        context,
        "auth_token_store",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override fun getToken(): String? = preferences.getString(KEY_TOKEN, null)

    override fun saveToken(token: String) {
        preferences.edit().putString(KEY_TOKEN, token).apply()
    }

    override fun clear() {
        preferences.edit().clear().apply()
    }

    override fun invalidate(httpCode: Int) {
        clear()
        _invalidations.tryEmit(AuthInvalidation(httpCode))
    }

    private companion object {
        const val KEY_TOKEN = "jwt"
    }
}
