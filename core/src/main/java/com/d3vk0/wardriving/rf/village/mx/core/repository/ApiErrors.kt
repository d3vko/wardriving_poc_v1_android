package com.d3vk0.wardriving.rf.village.mx.core.repository

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import retrofit2.HttpException

fun Throwable.toUserFacingMessage(fallback: String): String {
    if (this is UploadException) return message ?: fallback
    if (this is HttpException) {
        val serverMessage = response()?.errorBody()?.let { body ->
            runCatching { extractServerMessage(body.string()) }.getOrNull()
        }
        return httpMessage(code(), serverMessage, message())
    }
    return message?.takeIf { it.isNotBlank() } ?: fallback
}

internal fun httpMessage(code: Int, serverMessage: String?, fallback: String? = null): String {
    val detail = serverMessage?.takeIf { it.isNotBlank() }
        ?: fallback?.takeIf { it.isNotBlank() }
    return if (detail == null) "HTTP $code" else "HTTP $code: $detail"
}

internal fun extractServerMessage(rawBody: String): String? {
    if (rawBody.isBlank()) return null
    val root = runCatching { JsonParser.parseString(rawBody) }.getOrNull()
        ?: return rawBody.trim().take(300)
    return root.findMessage()?.take(300)
}

private fun JsonElement.findMessage(): String? = when {
    isJsonPrimitive -> asString
    isJsonArray -> asJsonArray.firstNotNullOfOrNull { it.findMessage() }
    isJsonObject -> {
        val body = asJsonObject
        listOf("message", "detail", "error", "non_field_errors")
            .firstNotNullOfOrNull { key -> body.get(key)?.findMessage() }
            ?: body.entrySet().firstNotNullOfOrNull { it.value.findMessage() }
    }
    else -> null
}
