package com.d3vk0.wardriving.rf.village.mx.core.telephony

class LteBandMapper {
    fun bandFromEarfcn(earfcn: Int?): String? {
        val value = earfcn ?: return null
        return when (value) {
            in 0..599 -> "1"
            in 600..1199 -> "2"
            in 1200..1949 -> "3"
            in 1950..2399 -> "4"
            in 2400..2649 -> "5"
            in 2750..3449 -> "7"
            in 3450..3799 -> "8"
            in 6150..6449 -> "20"
            in 9210..9659 -> "28"
            else -> null
        }
    }
}
