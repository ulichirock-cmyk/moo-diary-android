package com.moodiary.app.data

import com.moodiary.app.data.DeepSeekClient.firstMessage
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * Where the review paragraphs on 05 洞察 come from.
 *
 * The design shows a short, warm paragraph that quotes the week back at you — how many
 * entries, which days ran long, which tag keeps recurring. That is a language-model job,
 * so [DeepSeekInsightGenerator] asks deepseek-v4-flash for it, once per [ReviewPeriod].
 * The interface exists so the screen can be previewed, and the model swapped, without
 * touching the UI.
 */
interface InsightGenerator {
    /**
     * One paragraph reviewing [entries] (all dated within [range]) at the zoom level of
     * [period]. Throws [InsightException] with a user-facing message when the review
     * cannot be made.
     */
    suspend fun review(period: ReviewPeriod, entries: List<DiaryEntry>, range: ClosedRange<LocalDate>): String
}

/** Asks deepseek-v4-flash, via [DeepSeekClient], for one review paragraph. */
class DeepSeekInsightGenerator(private val apiKey: () -> String?) : InsightGenerator {

    override suspend fun review(
        period: ReviewPeriod,
        entries: List<DiaryEntry>,
        range: ClosedRange<LocalDate>,
    ): String {
        val key = apiKey()?.trim().orEmpty()
        if (key.isEmpty()) throw InsightException(NO_KEY)
        if (entries.isEmpty()) throw InsightException(NO_ENTRIES)

        val body = JSONObject()
            .put("model", DeepSeekClient.MODEL)
            // V4 thinks by default and the chain-of-thought counts against max_tokens, so a
            // short paragraph can come back as reasoning_content with an empty content.
            // A weekly review does not need it, and non-thinking mode is what makes
            // temperature take effect at all.
            .put("thinking", JSONObject().put("type", "disabled"))
            .put("temperature", 0.8)
            .put("max_tokens", period.maxTokens)
            .put(
                "messages",
                JSONArray()
                    .put(JSONObject().put("role", "system").put("content", systemPrompt(period)))
                    .put(JSONObject().put("role", "user").put("content", userPrompt(period, entries, range))),
            )

        val content = DeepSeekClient.chat(key, body).firstMessage().optString("content").trim()
        return DeepSeekClient.normalizePunctuation(content).ifEmpty { throw InsightException("DeepSeek 什么也没说") }
    }

    private fun userPrompt(period: ReviewPeriod, entries: List<DiaryEntry>, range: ClosedRange<LocalDate>): String =
        buildString {
            append("时间范围:").append(range.start).append(" 到 ").append(range.endInclusive).append('\n')
            val totalChars = entries.sumOf { it.text.trim().length }
            append("统计(第一句请直接引用这些数字):")
            append(entries.size).append(" 篇日记,").append(entries.photoCount()).append(" 张照片,")
            append("平均每篇约 ").append(totalChars / entries.size).append(" 字。\n")
            if (period.perEntryChars != Int.MAX_VALUE) {
                append("(篇幅所限,每篇正文只给出开头 ").append(period.perEntryChars).append(" 字)\n")
            }
            append('\n')
            entries.sortedBy { it.createdAt }.forEach { entry ->
                append("【").append(entry.date).append(' ').append(Weekday.of(entry.date))
                append(' ').append("%02d:%02d".format(entry.createdAt.hour, entry.createdAt.minute)).append("】")
                entry.place?.let { append(" 地点:").append(it) }
                if (entry.tags.isNotEmpty()) append(" 标签:").append(entry.tags.joinToString(" ") { "#$it" })
                if (entry.photos.isNotEmpty()) append(" 照片:").append(entry.photos.size).append("张")
                val text = entry.text.trim()
                append('\n')
                if (text.length > period.perEntryChars) append(text, 0, period.perEntryChars).append('…') else append(text)
                append("\n\n")
            }
        }

    private object Weekday {
        private val names = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        fun of(date: LocalDate) = names[date.dayOfWeek.value - 1]
    }

    companion object {
        const val NO_KEY = "no-key"
        const val NO_ENTRIES = "no-entries"

        /**
         * The reference paragraph from the design (05 洞察) is the whole spec for tone:
         * "这一周你写下 6 篇日记、11 张照片,平均每篇 180 字。周末的两篇写得最长,周一的加班日
         * 最短。「阅读」已连续三周出现在你的夜晚,或许值得给它一个固定的时段。"
         * The month and year cards keep that voice and only widen the lens.
         */
        private fun systemPrompt(period: ReviewPeriod) = """
            你是一本私人日记 App 里的「回顾」作者。用户会给你${period.label}的日记,你要写一段${period.label}的回顾。

            要求:
            - 用简体中文,第二人称「你」,语气温和、克制、像一位熟悉你的朋友,不要鸡汤,不要说教。
            - 只写一段,${period.minChars} 到 ${period.maxChars} 字。${period.maxChars} 字是硬上限,宁短勿长。不分点、不加标题、不用 Markdown、不用表情符号。
            - 第一句以「${period.label}你写了」开头,交代篇数、照片数和平均字数,数字直接用统计里给出的,不要自己重新数。
            - 接着从日记里提炼具体的观察:${period.focus}。要引用日记里真实出现的细节。
            - 最后给一句轻轻的、可选的建议,用「或许」「不妨」这类措辞。
            - 不要编造日记里没有的事,不要评价文笔。
            - 中文标点用全角,数字用阿拉伯数字,书名号「」用于引用标签或关键词。
        """.trimIndent()
    }
}
