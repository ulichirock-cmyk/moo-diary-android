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

    /** Inserts a new entry, or replaces the existing one with the same id. */
    fun upsert(entry: DiaryEntry)

    fun delete(id: String)
}

/**
 * Process-lifetime store seeded from [seedEntries]. Entries are kept newest-first so
 * the timeline can render the list directly, and so prev/next on the detail screen is
 * plain list adjacency.
 */
class InMemoryDiaryRepository(
    seed: List<DiaryEntry> = seedEntries(),
) : DiaryRepository {

    private val _entries = MutableStateFlow(seed.sortedByDescending { it.createdAt })
    override val entries: StateFlow<List<DiaryEntry>> = _entries.asStateFlow()

    override fun upsert(entry: DiaryEntry) {
        val without = _entries.value.filterNot { it.id == entry.id }
        _entries.value = (without + entry).sortedByDescending { it.createdAt }
    }

    override fun delete(id: String) {
        _entries.value = _entries.value.filterNot { it.id == id }
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
    tags: List<String>,
    place: String?,
    now: LocalDateTime = LocalDateTime.now(),
): DiaryEntry = DiaryEntry(
    id = "entry-${now.toLocalDate()}-${System.currentTimeMillis()}",
    createdAt = now,
    text = text.trim(),
    photos = photos,
    tags = tags,
    place = place,
)
