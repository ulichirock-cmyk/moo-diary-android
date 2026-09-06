package com.moodiary.app.data

import androidx.compose.runtime.Immutable
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * One piece of an entry's body, in reading order. Prose and photos interleave freely
 * (文中图): a sentence, a photo, two more photos, another paragraph.
 */
@Immutable
sealed interface Block {
    data class Text(val text: String) : Block

    /**
     * [uri] is an `https://` sample URL (seed data) or a `file://` copy made by [PhotoStore].
     * [caption] is the small line under the photo (长按照片 → 标注), or null for none.
     */
    data class Photo(val uri: String, val caption: String? = null) : Block
}

/**
 * One diary entry. The body is [blocks]; [text] and [photos] are views over it that
 * the rest of the app — search, stats, the timeline card, the AI tools — keeps reading,
 * because for all of them a photo's position in the prose is beside the point.
 *
 * There is deliberately no mood field: the design dropped moods entirely in favour of
 * places (see design/SYNC.md).
 */
@Immutable
data class DiaryEntry(
    val id: String,
    val createdAt: LocalDateTime,
    val blocks: List<Block>,
    val tags: List<String> = emptyList(),
    val place: String? = null,
) {
    val date: LocalDate get() = createdAt.toLocalDate()

    /**
     * The prose alone: text blocks trimmed and joined with a blank line, photos left out.
     *
     * Computed once, not on every read: a timeline card reads [text] and [photos] while
     * it is being composed mid-fling, and a fresh walk of [blocks] there costs both the
     * allocation and the skip — the new List instance made a card's photo block look
     * changed to Compose every time.
     */
    val text: String by lazy {
        blocks.filterIsInstance<Block.Text>()
            .map { it.text.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n\n")
    }

    /** Every photo in reading order. */
    val photos: List<String> by lazy {
        blocks.filterIsInstance<Block.Photo>().map { it.uri }
    }
}

/**
 * The pre-块 shape — one text, then the photos — for callers that have no positions
 * to give: seed data, `newEntry`, and rows written before the `blocks` column existed.
 */
fun DiaryEntry(
    id: String,
    createdAt: LocalDateTime,
    text: String,
    photos: List<String> = emptyList(),
    tags: List<String> = emptyList(),
    place: String? = null,
): DiaryEntry = DiaryEntry(
    id = id,
    createdAt = createdAt,
    blocks = blocksOf(text, photos),
    tags = tags,
    place = place,
)

/** `[Text(text)] + photos`, with an empty text left out. */
fun blocksOf(text: String, photos: List<String>): List<Block> =
    buildList {
        if (text.isNotBlank()) add(Block.Text(text.trim()))
        photos.forEach { add(Block.Photo(it)) }
    }
