package com.moodiary.app.data

import java.time.LocalDate

/** The number of consecutive days, counting back from [today], that have an entry. */
fun List<DiaryEntry>.streak(today: LocalDate = LocalDate.now()): Int {
    val days = mapTo(HashSet()) { it.date }
    if (days.isEmpty()) return 0
    // A streak is still alive if you wrote yesterday but not yet today.
    var cursor = if (today in days) today else today.minusDays(1)
    if (cursor !in days) return 0
    var count = 0
    while (cursor in days) {
        count++
        cursor = cursor.minusDays(1)
    }
    return count
}

/** The mood that appears most on [date]; ties break toward the latest entry. */
fun List<DiaryEntry>.dominantMood(date: LocalDate): Mood? =
    filter { it.date == date }
        .mapNotNull { it.mood }
        .groupingBy { it }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key

/** Tag -> use count, most used first. */
fun List<DiaryEntry>.tagCounts(): List<Pair<String, Int>> =
    flatMap { it.tags }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .map { it.key to it.value }

fun List<DiaryEntry>.photoCount(): Int = sumOf { it.photos.size }

fun List<DiaryEntry>.datesWithEntries(): Set<LocalDate> = mapTo(HashSet()) { it.date }

/**
 * Free-text search over body and tags. Returns entries newest-first; the caller
 * highlights [query] inside the body itself.
 */
fun List<DiaryEntry>.search(query: String): List<DiaryEntry> {
    val q = query.trim()
    if (q.isEmpty()) return emptyList()
    return filter { entry ->
        entry.text.contains(q, ignoreCase = true) ||
            entry.tags.any { it.contains(q, ignoreCase = true) }
    }
}
