package com.d3vk0.wardriving.rf.village.mx.core.security

import okhttp3.CertificatePinner

/**
 * Pins SPKI SHA-256 para `*.rf-village-mx.com` (hoja + YE1 + ISRG X2).
 * Extraídos 2026-10-31; si Let's Encrypt rota la hoja, actualizar pin-set XML y esta lista.
 */
object TlsPins {
    const val DOMAIN = "rf-village-mx.com"

    val SPKI_SHA256_BASE64 = listOf(
        "VUwGnQVqxR8x/NGJqYOufLnpjwBR3B6RCI2AsBduMXQ=",
        "brzvtCELCIZUo4sD/qPX0ccRtPsd3DY6RfmxpOU9oB4=",
        "diGVwiVYbubAI3RW4hB9xU8e/CH2GnkuvVFZE8zmgzI=",
    )

    fun certificatePinner(): CertificatePinner {
        val pins = SPKI_SHA256_BASE64.map { "sha256/$it" }.toTypedArray()
        return CertificatePinner.Builder()
            .add(DOMAIN, *pins)
            .add("*.$DOMAIN", *pins)
            .build()
    }

    fun isPinnedHost(host: String): Boolean {
        val normalized = host.lowercase().trim()
        return normalized == DOMAIN || normalized.endsWith(".$DOMAIN")
    }
}
