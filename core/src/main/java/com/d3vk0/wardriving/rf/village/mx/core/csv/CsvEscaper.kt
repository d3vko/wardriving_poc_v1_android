package com.d3vk0.wardriving.rf.village.mx.core.csv

internal object CsvEscaper {
    fun escape(value: Any?): String {
        val text = value?.toString().orEmpty()
        val needsQuotes = text.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        val escaped = text.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }
}
