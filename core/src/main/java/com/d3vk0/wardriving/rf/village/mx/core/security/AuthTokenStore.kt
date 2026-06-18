package com.d3vk0.wardriving.rf.village.mx.core.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

interface AuthTokenStorage {
    fun getToken(): String?
    fun saveToken(token: String)
    fun clear()
}

class AuthTokenStore(context: Context) : AuthTokenStorage {
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

    private companion object {
        const val KEY_TOKEN = "jwt"
    }
}
