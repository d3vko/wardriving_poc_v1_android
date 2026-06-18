package com.d3vk0.wardriving.rf.village.mx.core.duplicate

class DuplicateFilter(
    private val windowMillis: Long = 20_000L,
) {
    private val lastSeen = linkedMapOf<String, Long>()

    fun shouldKeep(key: String, timestamp: Long): Boolean {
        val previous = lastSeen[key]
        if (previous != null && timestamp - previous < windowMillis) return false
        lastSeen[key] = timestamp
        if (lastSeen.size > 2_000) {
            val cutoff = timestamp - windowMillis * 3
            lastSeen.entries.removeIf { it.value < cutoff }
        }
        return true
    }
}
