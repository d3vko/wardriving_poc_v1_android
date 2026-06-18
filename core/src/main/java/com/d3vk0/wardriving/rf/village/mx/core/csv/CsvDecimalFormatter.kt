package com.d3vk0.wardriving.rf.village.mx.core.csv

import java.util.Locale

internal object CsvDecimalFormatter {
    private const val LatitudeIntegerDigits = 2
    private const val LongitudeIntegerDigits = 3
    private const val MeasurementIntegerDigits = 8
    private const val MhzIntegerDigits = 4

    fun latitude(value: Double?): String = decimal(value, decimalPlaces = 7, maxIntegerDigits = LatitudeIntegerDigits)

    fun longitude(value: Double?): String = decimal(value, decimalPlaces = 7, maxIntegerDigits = LongitudeIntegerDigits)

    fun measurement(value: Double?): String = decimal(value, decimalPlaces = 2, maxIntegerDigits = MeasurementIntegerDigits)

    fun measurement(value: Float?): String = measurement(value?.toDouble())

    fun mhz(value: Double?): String = decimal(value, decimalPlaces = 1, maxIntegerDigits = MhzIntegerDigits)

    private fun decimal(value: Double?, decimalPlaces: Int, maxIntegerDigits: Int): String {
        if (value == null || !value.isFinite()) return ""

        val formatted = String.format(Locale.US, "%.${decimalPlaces}f", value)
        return formatted.takeIf { it.integerDigitCount() <= maxIntegerDigits }.orEmpty()
    }

    private fun String.integerDigitCount(): Int {
        val integerPart = substringBefore('.').removePrefix("-")
        return integerPart.trimStart('0').ifEmpty { "0" }.length
    }
}
