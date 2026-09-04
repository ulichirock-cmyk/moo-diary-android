package com.moodiary.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

/**
 * Single view model for the whole app. Moodiary is one small pile of state — the
 * entries plus a draft — and keeping it in one place is what lets the editor survive
 * being covered by the place picker and come back intact.
 */
class DiaryViewModel : ViewModel() {

    private val repository: DiaryRepository = InMemoryDiaryRepository.shared
    private val placeSource: PlaceSource = StubPlaceSource
    private val updateChecker: UpdateChecker = StubUpdateChecker

    val entries get() = repository.entries

    // ── Editor draft ─────────────────────────────────────────────────────────
    /**
     * Non-null while editing an existing entry (更多操作 → 编辑这篇日记). Publishing
     * then replaces that entry instead of adding one, and keeps its original date.
     */
    var editingId by mutableStateOf<String?>(null)
        private set
    private var editingCreatedAt: LocalDateTime? = null

    var draftText by mutableStateOf("")
        private set
    var draftPlace by mutableStateOf<String?>(null)
        private set
    val draftPhotos = mutableStateListOf<String>()
    val draftTags = mutableStateListOf<String>()

    /** The timestamp the editor header shows: the entry's own when editing, else now. */
    fun draftTimestamp(fallback: LocalDateTime): LocalDateTime = editingCreatedAt ?: fallback

    val canPublish: Boolean
        get() = draftText.isNotBlank() || draftPhotos.isNotEmpty()

    fun onDraftTextChange(value: String) {
        draftText = value
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

    /** Opens the editor on a blank draft. */
    fun startNewEntry() {
        clearDraft()
    }

    /**
     * Loads [entry] into the draft. Any in-progress new draft is replaced — the app
     * keeps one draft, not two, and the alternative (stashing) is state nobody asked
     * for.
     */
    fun startEditing(entry: DiaryEntry) {
        editingId = entry.id
        editingCreatedAt = entry.createdAt
        draftText = entry.text
        draftPlace = entry.place
        draftPhotos.clear()
        draftPhotos.addAll(entry.photos)
        draftTags.clear()
        draftTags.addAll(entry.tags)
    }

    fun clearDraft() {
        editingId = null
        editingCreatedAt = null
        draftText = ""
        draftPlace = null
        draftPhotos.clear()
        draftTags.clear()
    }

    /** Saves the draft and clears it. No-op when [canPublish] is false. */
    fun publishDraft() {
        if (!canPublish) return
        val existingId = editingId
        val entry = if (existingId != null) {
            DiaryEntry(
                id = existingId,
                createdAt = editingCreatedAt ?: LocalDateTime.now(),
                text = draftText.trim(),
                photos = draftPhotos.toList(),
                tags = draftTags.toList(),
                place = draftPlace,
            )
        } else {
            newEntry(
                text = draftText,
                photos = draftPhotos.toList(),
                tags = draftTags.toList(),
                place = draftPlace,
            )
        }
        repository.upsert(entry)
        clearDraft()
    }

    fun deleteEntry(id: String) {
        repository.delete(id)
    }

    /** Tags offered in the editor: the ones already in use, then the suggestions. */
    fun editorTagOptions(): List<String> {
        val used = entries.value.tagCounts().map { it.first }
        return (draftTags + used + SUGGESTED_TAGS).distinct().take(12)
    }

    // ── Place picker ─────────────────────────────────────────────────────────
    var nearbyPlaces by mutableStateOf<List<PlaceSuggestion>>(emptyList())
        private set
    var pinPlaces by mutableStateOf<List<PlaceSuggestion>>(emptyList())
        private set

    /** Name currently highlighted on 地点选择 / 地图选点 before 完成 commits it. */
    var pendingPlace by mutableStateOf<String?>(null)
        private set

    var placeQuery by mutableStateOf("")
        private set

    fun openPlacePicker() {
        pendingPlace = draftPlace
        placeQuery = ""
        viewModelScope.launch { nearbyPlaces = placeSource.nearby() }
    }

    fun openMapPicker() {
        viewModelScope.launch {
            pinPlaces = placeSource.atPin()
            if (pendingPlace == null) pendingPlace = pinPlaces.firstOrNull()?.name
        }
    }

    fun onPlaceQueryChange(value: String) {
        placeQuery = value
    }

    fun selectPlace(name: String?) {
        pendingPlace = name
    }

    /** 完成 / 用这个地点 — pushes the highlighted name onto the draft. */
    fun commitPlace() {
        draftPlace = pendingPlace?.takeIf { it.isNotBlank() }
    }

    /** 常去 chips, most used first. */
    fun frequentPlaces(): List<String> = entries.value.placeCounts().take(6).map { it.first }

    // ── Update check ─────────────────────────────────────────────────────────
    var availableUpdate by mutableStateOf<UpdateInfo?>(null)
        private set

    fun checkForUpdate(currentVersion: String) {
        viewModelScope.launch { availableUpdate = updateChecker.check(currentVersion) }
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
