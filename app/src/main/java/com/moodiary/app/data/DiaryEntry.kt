package com.moodiary.app.data

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * One diary entry. [photos] holds either an `https://` sample URL (seed data) or a
 * `content://` URI handed back by the system photo picker — Coil loads both.
 */
data class DiaryEntry(
    val id: String,
    val createdAt: LocalDateTime,
    val text: String,
    val photos: List<String> = emptyList(),
    val mood: Mood? = null,
    val tags: List<String> = emptyList(),
    val place: String? = null,
) {
    val date: LocalDate get() = createdAt.toLocalDate()
}
