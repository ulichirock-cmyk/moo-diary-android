package com.moodiary.app.data

import com.moodiary.app.data.DeepSeekClient.firstMessage
import org.json.JSONArray
import org.json.JSONObject
import com.moodiary.app.ui.Fmt
import java.time.LocalDate

/**
 * 写作引导: one short question, the placeholder of a blank new entry, written by DeepSeek from a
 * light sketch of the diary — recent dates, tags and places, never the text. The
 * question is meant for the kind of writer who notices everyone's feelings but their
 * own: it asks about the writer, not about the day's events. Returns null on any
 * failure; the view model then falls back to [FALLBACK].
 */
class WritingPromptSuggester(private val apiKey: () -> String?) {

    suspend fun suggest(recent: List<DiaryEntry>, today: LocalDate, avoid: List<String>): String? {
        val key = apiKey()?.trim().orEmpty()
        if (key.isEmpty()) return null

        val body = JSONObject()
            .put("model", DeepSeekClient.MODEL)
            .put("thinking", JSONObject().put("type", "disabled"))
            .put("temperature", 1.0)
            .put("max_tokens", 80)
            .put("response_format", JSONObject().put("type", "json_object"))
            .put(
                "messages",
                JSONArray()
                    .put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
                    .put(JSONObject().put("role", "user").put("content", userPrompt(recent, today, avoid))),
            )

        return runCatching {
            val content = DeepSeekClient.chat(key, body).firstMessage().optString("content")
            JSONObject(content).optString("question").trim()
                .takeIf { it.isNotEmpty() && it.length <= MAX_CHARS }
        }.getOrNull()
    }

    private fun userPrompt(recent: List<DiaryEntry>, today: LocalDate, avoid: List<String>) = buildString {
        append("今天是 ").append(today).append(',').append(Fmt.weekday(today)).append("。\n")
        if (recent.isEmpty()) {
            append("这本日记还是空的。\n")
        } else {
            append("最近几篇日记(只有日期、标签、地点,没有正文):\n")
            recent.forEach { e ->
                append("- ").append(e.date)
                if (e.tags.isNotEmpty()) append(" 标签:").append(e.tags.joinToString("、"))
                e.place?.let { append(" 地点:").append(it) }
                append('\n')
            }
        }
        if (avoid.isNotEmpty()) {
            append("最近已经问过,不要再问类似的:\n")
            avoid.forEach { append("- ").append(it).append('\n') }
        }
    }

    companion object {
        const val MAX_CHARS = 28

        /** Used when there is no key, no network, or the model came back with nothing usable. */
        val FALLBACK = listOf(
            "今天哪件事,其实不是你的事?",
            "今天有哪一刻,你只是在替别人难过?",
            "今天什么时候你觉得电量回来了一点?",
            "有没有一句话,你想说但没说出口?",
            "今天你看到了什么,而不是想到了什么?",
            "你今天有一个预感,是什么?",
            "今天有哪件小事,你不想让它过去?",
        )

        private val SYSTEM_PROMPT = """
            你为一本私人日记 App 的空白编辑器写一句「写作引导」。用户打开编辑器还没落笔,你的问题会作为输入框的占位文字显示。
            用户是那种很会体察别人、却常常忽略自己的人。所以问题要把镜头转回用户自己:今天的感受、消耗、边界、直觉、身体、说没说出口的话,而不是问「今天发生了什么」。
            要求:
            - 只问一个问题,简体中文,不超过 $MAX_CHARS 个字,以问号结尾。
            - 具体、日常、可以立刻回答;不要抽象的大词,不要鸡汤,不要「你觉得幸福吗」这种。
            - 语气像一个熟悉你的朋友随口一问,不要说教。
            - 可以参考最近的标签和地点,但不要编造正文里的事。
            - 只输出 JSON:{"question":"..."}
        """.trimIndent()
    }
}
