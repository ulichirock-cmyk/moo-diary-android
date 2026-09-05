package com.moodiary.app.data

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

/**
 * Single view model for the whole app. Moodiary is one small pile of state — the
 * entries plus a draft — and keeping it in one place is what lets the editor survive
 * being covered by the place picker and come back intact.
 */
class DiaryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DiaryRepository = RoomDiaryRepository.get(application)
    private val photoStore = PhotoStore(application)
    // Reverse geocoding is a platform service, so it needs the application context.
    private val placeSource: PlaceSource = GeocoderPlaceSource(application)
    private val updateChecker: UpdateChecker = StubUpdateChecker
    private val aiSettings = AiSettings(application)
    private val insightCache = InsightCache(application)
    private val insightGenerator: InsightGenerator = DeepSeekInsightGenerator { aiSettings.apiKey }
    private val assistant = DiaryAssistant { aiSettings.apiKey }

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

    /**
     * Copies the picked photos into app storage first (see [PhotoStore]) — the picker's
     * URIs would not survive a restart — then attaches the copies to the draft.
     */
    fun addDraftPhotos(uris: List<String>) {
        viewModelScope.launch {
            photoStore.import(uris).forEach { if (it !in draftPhotos) draftPhotos.add(it) }
        }
    }

    fun removeDraftPhoto(uri: String) {
        draftPhotos.remove(uri)
        releasePhotos(listOf(uri))
    }

    /** Deletes local copies that no saved entry refers to any more. */
    private fun releasePhotos(uris: Collection<String>) {
        val inUse = entries.value.flatMapTo(HashSet()) { it.photos }
        photoStore.delete(uris.filterNot { it in inUse })
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
        releasePhotos(draftPhotos.toList())
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
        // Photos dropped while editing are nobody's now.
        entries.value.firstOrNull { it.id == entry.id }
            ?.let { old -> photoStore.delete(old.photos - entry.photos.toSet()) }
        repository.upsert(entry)
        // The remaining copies belong to the entry; the write is asynchronous, so hand
        // them off before clearDraft() gets a chance to treat them as orphans.
        draftPhotos.clear()
        clearDraft()
    }

    fun deleteEntry(id: String) {
        val entry = entries.value.firstOrNull { it.id == id }
        repository.delete(id)
        entry?.let { photoStore.delete(it.photos) }
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

    /**
     * Selection to restore if the map is dismissed with 返回. Moving the pin overwrites
     * [pendingPlace] — that is the whole point of the screen — so backing out of it has
     * to be able to undo that.
     */
    private var placeBeforeMap: String? = null
    private var pinJob: Job? = null

    fun openMapPicker() {
        placeBeforeMap = pendingPlace
        pinPlaces = emptyList()
    }

    /**
     * Names the coordinate under the pin. Called by 地图选点 once the camera has stopped
     * moving, so a drag across the city geocodes once rather than sixty times.
     */
    fun resolvePin(lat: Double, lng: Double) {
        pinJob?.cancel()
        pinJob = viewModelScope.launch {
            val places = placeSource.atPin(lat, lng)
            pinPlaces = places
            pendingPlace = places.firstOrNull()?.name ?: pendingPlace
        }
    }

    /** 返回 from the map: the pin never happened. */
    fun cancelMapPick() {
        pinJob?.cancel()
        pendingPlace = placeBeforeMap
        pinPlaces = emptyList()
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

    // ── Insights ─────────────────────────────────────────────────────────────
    /** What one review card on 05 洞察 shows. */
    sealed interface InsightState {
        data object Idle : InsightState
        data object Loading : InsightState
        data object NoKey : InsightState
        data object NoEntries : InsightState
        data class Ready(val text: String) : InsightState
        data class Error(val message: String) : InsightState
    }

    /** One state per card; missing keys read as [InsightState.Idle]. */
    val insights = mutableStateMapOf<ReviewPeriod, InsightState>()

    /** Fingerprint of the entries each card's current text was generated from. */
    private val insightSources = HashMap<ReviewPeriod, String>()
    private val insightJobs = HashMap<ReviewPeriod, Job>()

    fun refreshInsights(force: Boolean = false) {
        ReviewPeriod.entries.forEach { refreshInsight(it, force) }
    }

    /**
     * Generates the review for [period]. Cheap to call on every visit to the tab: it
     * re-runs only when that span's entries changed or [force] is set, so flipping tabs
     * does not burn tokens.
     */
    fun refreshInsight(period: ReviewPeriod, force: Boolean = false) {
        val range = period.range()
        val span = entries.value.filter { it.date in range }
        val fingerprint = span.joinToString("|") {
            "${it.id}:${it.text.hashCode()}:${it.tags}:${it.place}:${it.photos.size}"
        }
        val current = insights[period] ?: InsightState.Idle

        // First look in this process: a review generated last time for exactly these
        // entries is still right, so show it without a request.
        if (!force && insightSources[period] == null) {
            insightCache[period]?.takeIf { it.fingerprint == fingerprint }?.let { cached ->
                insightSources[period] = fingerprint
                insights[period] = InsightState.Ready(cached.text)
                return
            }
        }

        val stale = fingerprint != insightSources[period]
        val settled = current is InsightState.Ready || current is InsightState.NoEntries
        if (!force && !stale && (settled || current is InsightState.Loading)) return
        // A missing key is not a reason to keep retrying on every visit either.
        if (!force && !stale && current is InsightState.NoKey && aiSettings.apiKey == null) return

        insightJobs[period]?.cancel()
        insightSources[period] = fingerprint
        if (span.isEmpty()) {
            insights[period] = InsightState.NoEntries
            return
        }
        insights[period] = InsightState.Loading
        insightJobs[period] = viewModelScope.launch {
            insights[period] = try {
                val text = insightGenerator.review(period, span, range)
                insightCache.put(period, fingerprint, text)
                InsightState.Ready(text)
            } catch (e: InsightException) {
                when (e.message) {
                    DeepSeekInsightGenerator.NO_KEY -> InsightState.NoKey
                    DeepSeekInsightGenerator.NO_ENTRIES -> InsightState.NoEntries
                    else -> InsightState.Error(e.message ?: "未知错误")
                }
            }
        }
    }

    // ── 问问日记 ────────────────────────────────────────────────────────────
    /** The conversation, oldest first. Lives with the process; nothing is written down. */
    val chatMessages = mutableStateListOf<ChatMessage>()

    var chatInput by mutableStateOf("")
        private set
    var chatBusy by mutableStateOf(false)
        private set

    fun onChatInputChange(value: String) {
        chatInput = value
    }

    fun sendChat(question: String = chatInput) {
        val q = question.trim()
        if (q.isEmpty() || chatBusy) return
        chatInput = ""
        val history = chatMessages.filterNot { it.isError }
        chatMessages += ChatMessage(fromUser = true, text = q)
        chatBusy = true
        viewModelScope.launch {
            chatMessages += try {
                assistant.ask(history, q, entries.value)
            } catch (e: InsightException) {
                val text = if (e.message == DiaryAssistant.NO_KEY) {
                    "还没有配置 AI。去「我的 → AI 洞察」填入 DeepSeek API Key 再来问。"
                } else {
                    e.message ?: "未知错误"
                }
                ChatMessage(fromUser = false, text = text, isError = true)
            }
            chatBusy = false
        }
    }

    fun clearChat() {
        chatMessages.clear()
    }

    // ── AI settings ──────────────────────────────────────────────────────────
    var hasApiKey by mutableStateOf(aiSettings.apiKey != null)
        private set

    fun saveApiKey(key: String) {
        aiSettings.apiKey = key.takeIf { it.isNotBlank() }
        hasApiKey = aiSettings.apiKey != null
        // The key changed, so whatever the cards show (NoKey, an auth error) is stale.
        insightSources.clear()
        insights.clear()
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
