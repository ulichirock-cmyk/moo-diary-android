package com.moodiary.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime

/**
 * Everything the UI reads and writes goes through here. Keeping it an interface means
 * swapping the in-memory store for Room (or a file-backed Markdown store, which is
 * what "导出 Markdown" implies) touches exactly one class.
 */
interface DiaryRepository {
    val entries: StateFlow<List<DiaryEntry>>

    fun add(entry: DiaryEntry)
}

/**
 * Process-lifetime store seeded from [seedEntries]. Entries are kept newest-first so
 * the timeline can render the list directly.
 */
class InMemoryDiaryRepository(
    seed: List<DiaryEntry> = seedEntries(),
) : DiaryRepository {

    private val _entries = MutableStateFlow(seed.sortedByDescending { it.createdAt })
    override val entries: StateFlow<List<DiaryEntry>> = _entries.asStateFlow()

    override fun add(entry: DiaryEntry) {
        _entries.value = (_entries.value + entry).sortedByDescending { it.createdAt }
    }

    companion object {
        /**
         * Single shared instance. The app has one screenful of state and no DI
         * framework; a plain object holder keeps entries alive across configuration
         * changes and across the editor -> timeline hop.
         */
        val shared: InMemoryDiaryRepository by lazy { InMemoryDiaryRepository() }
    }
}

/** Builds an entry with a fresh id and the current timestamp. */
fun newEntry(
    text: String,
    photos: List<String>,
    mood: Mood?,
    tags: List<String>,
    now: LocalDateTime = LocalDateTime.now(),
): DiaryEntry = DiaryEntry(
    id = "entry-${now.toLocalDate()}-${System.currentTimeMillis()}",
    createdAt = now,
    text = text.trim(),
    photos = photos,
    mood = mood,
    tags = tags,
)
