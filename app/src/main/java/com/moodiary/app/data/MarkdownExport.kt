package com.moodiary.app.data

import com.moodiary.app.ui.Fmt

/**
 * Renders the whole diary as one Markdown document — the format the "导出 Markdown"
 * row promises, and the same shape Claude reads over MCP.
 */
fun List<DiaryEntry>.toMarkdown(owner: String = OWNER_NAME): String = buildString {
    appendLine("# $owner 的 moodiary")
    appendLine()
    appendLine("共 ${this@toMarkdown.size} 篇日记 · ${this@toMarkdown.photoCount()} 张照片")
    appendLine()
    this@toMarkdown.sortedByDescending { it.createdAt }.forEach { appendEntry(it) }
}

/** Single-entry export, used by 更多操作 on the detail screen. */
fun DiaryEntry.toMarkdown(): String = buildString { appendEntry(this@toMarkdown) }

private fun StringBuilder.appendEntry(entry: DiaryEntry) {
    appendLine("## ${entry.date} ${Fmt.weekday(entry.date)} ${Fmt.time(entry.createdAt)}")
    appendLine()
    val meta = buildList {
        entry.place?.let { add("地点: $it") }
        if (entry.tags.isNotEmpty()) add("标签: " + entry.tags.joinToString(" ") { "#$it" })
    }
    if (meta.isNotEmpty()) {
        appendLine("> " + meta.joinToString(" · "))
        appendLine()
    }
    if (entry.text.isNotBlank()) {
        appendLine(entry.text)
        appendLine()
    }
    entry.photos.forEachIndexed { index, photo ->
        appendLine("![照片 ${index + 1}]($photo)")
    }
    if (entry.photos.isNotEmpty()) appendLine()
}
