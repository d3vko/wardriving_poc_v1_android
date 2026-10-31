package com.d3vk0.wardriving.rf.village.mx.core.security

import java.util.Base64

object JwtClaims {
    fun username(token: String): String? {
        val parts = token.split('.')
        if (parts.size < 2) return null
        val json = runCatching {
            val padded = padBase64Url(parts[1])
            String(Base64.getUrlDecoder().decode(padded), Charsets.UTF_8)
        }.getOrNull() ?: return null
        return listOf("username", "preferred_username", "user").firstNotNullOfOrNull { key ->
            Regex("\"$key\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
        }
    }

    private fun padBase64Url(value: String): String {
        val pad = (4 - value.length % 4) % 4
        return value + "=".repeat(pad)
    }
}
