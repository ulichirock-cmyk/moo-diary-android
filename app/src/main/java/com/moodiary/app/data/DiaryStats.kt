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

/** Tag -> use count, most used first. */
fun List<DiaryEntry>.tagCounts(): List<Pair<String, Int>> = countBy { it.tags }

/** Place -> use count, most used first. Drives the 常去 chips on the place picker. */
fun List<DiaryEntry>.placeCounts(): List<Pair<String, Int>> =
    countBy { entry -> entry.place?.let(::listOf).orEmpty() }

private fun List<DiaryEntry>.countBy(selector: (DiaryEntry) -> List<String>): List<Pair<String, Int>> =
    flatMap(selector)
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
            entry.tags.any { it.contains(q, ignoreCase = true) } ||
            entry.place?.contains(q, ignoreCase = true) == true
    }
}

/**
 * The entry written just before [entry], i.e. the one the detail screen's "‹ 9月1日"
 * link goes to. The list is newest-first, so that is the *next* element.
 */
fun List<DiaryEntry>.olderThan(entry: DiaryEntry): DiaryEntry? {
    val index = indexOfFirst { it.id == entry.id }
    return if (index < 0) null else getOrNull(index + 1)
}

/** The entry written just after [entry]; null when [entry] is the newest. */
fun List<DiaryEntry>.newerThan(entry: DiaryEntry): DiaryEntry? {
    val index = indexOfFirst { it.id == entry.id }
    return if (index <= 0) null else getOrNull(index - 1)
}
