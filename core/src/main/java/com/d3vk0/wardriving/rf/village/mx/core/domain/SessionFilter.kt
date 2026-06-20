package com.d3vk0.wardriving.rf.village.mx.core.domain

import com.d3vk0.wardriving.rf.village.mx.core.local.WardrivingSessionEntity

enum class SessionFilter(val label: String) {
    ALL("Todas"),
    PROCESSED("En plataforma"),
    UNPROCESSED("Solo local"),
    ;

    companion object {
        fun fromStorageValue(value: String?): SessionFilter =
            entries.firstOrNull { it.name == value } ?: ALL
    }
}

fun SessionFilter.toStorageValue(): String = name

fun filterSessions(
    sessions: List<WardrivingSessionEntity>,
    filter: SessionFilter,
): List<WardrivingSessionEntity> = sessions
    .asSequence()
    .filter { session ->
        when (filter) {
            SessionFilter.ALL -> true
            SessionFilter.PROCESSED -> session.uploaded
            SessionFilter.UNPROCESSED -> !session.uploaded
        }
    }
    .sortedByDescending(WardrivingSessionEntity::startedAt)
    .toList()
