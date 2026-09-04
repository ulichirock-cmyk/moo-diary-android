package com.moodiary.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.time.LocalDate
import java.time.YearMonth

/**
 * Single view model for the whole app. Moodiary is one small pile of state — the
 * entries plus a draft — and keeping it in one place is what makes "草稿已自动保存"
 * true: leaving the editor and coming back finds the draft intact.
 */
class DiaryViewModel : ViewModel() {

    private val repository: DiaryRepository = InMemoryDiaryRepository.shared

    val entries get() = repository.entries

    // ── Editor draft ─────────────────────────────────────────────────────────
    var draftText by mutableStateOf("")
        private set
    var draftMood by mutableStateOf<Mood?>(null)
        private set
    val draftPhotos = mutableStateListOf<String>()
    val draftTags = mutableStateListOf<String>()

    /** True once the draft holds anything worth telling the user we kept. */
    val draftIsDirty: Boolean
        get() = draftText.isNotBlank() || draftPhotos.isNotEmpty() ||
            draftMood != null || draftTags.isNotEmpty()

    val canPublish: Boolean
        get() = draftText.isNotBlank() || draftPhotos.isNotEmpty()

    fun onDraftTextChange(value: String) {
        draftText = value
    }

    fun onDraftMoodClick(mood: Mood) {
        draftMood = if (draftMood == mood) null else mood
    }

    fun onDraftTagClick(tag: String) {
        if (!draftTags.remove(tag)) draftTags.add(tag)
    }

    fun addDraftPhotos(uris: List<String>) {
        uris.forEach { if (it !in draftPhotos) draftPhotos.add(it) }
    }

    fun removeDraftPhoto(uri: String) {
        draftPhotos.remove(uri)
    }

    fun discardDraft() {
        draftText = ""
        draftMood = null
        draftPhotos.clear()
        draftTags.clear()
    }

    /** Publishes the draft and clears it. No-op when [canPublish] is false. */
    fun publishDraft() {
        if (!canPublish) return
        repository.add(
            newEntry(
                text = draftText,
                photos = draftPhotos.toList(),
                mood = draftMood,
                tags = draftTags.toList(),
            ),
        )
        discardDraft()
    }

    /** Tags offered in the editor: the ones already in use, then the suggestions. */
    fun editorTagOptions(): List<String> {
        val used = entries.value.tagCounts().map { it.first }
        return (draftTags + used + SUGGESTED_TAGS).distinct().take(12)
    }

    // ── Calendar ─────────────────────────────────────────────────────────────
    var visibleMonth by mutableStateOf(YearMonth.now())
        private set
    var selectedDate by mutableStateOf(LocalDate.now())
        private set

    fun showMonth(month: YearMonth) {
        visibleMonth = month
        // Keep the selection inside the month the user is looking at.
        if (YearMonth.from(selectedDate) != month) {
            selectedDate = if (month == YearMonth.now()) LocalDate.now() else month.atDay(1)
        }
    }

    fun selectDate(date: LocalDate) {
        selectedDate = date
        visibleMonth = YearMonth.from(date)
    }

    // ── Search ───────────────────────────────────────────────────────────────
    var searchQuery by mutableStateOf("")
        private set

    fun onSearchQueryChange(value: String) {
        searchQuery = value
    }

    fun clearSearch() {
        searchQuery = ""
    }
}
