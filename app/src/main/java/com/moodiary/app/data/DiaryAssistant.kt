package com.moodiary.app.data

import com.moodiary.app.data.DeepSeekClient.firstMessage
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** One turn of 问问日记. [refs] are ids of entries the answer cites, in order. */
data class ChatMessage(
    val fromUser: Boolean,
    val text: String,
    val refs: List<String> = emptyList(),
    val isError: Boolean = false,
)

/**
 * The read-only assistant behind 问问日记: "when did I last go to 青岛", "how many runs
 * this year", "the entry about 小七's wedding".
 *
 * The diary is never pasted into the prompt. The model gets two tools — search and
 * fetch — and calls them as it likes, so a few hundred entries cost the same as ten,
 * and an answer can only cite entries it actually looked up. Citations come back as
 * `[[entry-id]]` markers, which the UI turns into tappable date chips.
 */
class DiaryAssistant(private val apiKey: () -> String?) {

    suspend fun ask(
        history: List<ChatMessage>,
        question: String,
        entries: List<DiaryEntry>,
        today: LocalDate = LocalDate.now(),
    ): ChatMessage {
        val key = apiKey()?.trim().orEmpty()
        if (key.isEmpty()) throw InsightException(NO_KEY)

        val messages = JSONArray().put(message("system", systemPrompt(today, entries)))
        history.takeLast(HISTORY_TURNS).forEach {
            messages.put(message(if (it.fromUser) "user" else "assistant", it.text))
        }
        messages.put(message("user", question))

        repeat(MAX_TOOL_ROUNDS) {
            val reply = DeepSeekClient.chat(key, request(messages)).firstMessage()
            val calls = reply.optJSONArray("tool_calls")
            if (calls == null || calls.length() == 0) {
                return parseAnswer(reply.optString("content"), entries)
            }
            messages.put(reply)
            for (i in 0 until calls.length()) {
                val call = calls.getJSONObject(i)
                val function = call.getJSONObject("function")
                val args = runCatching { JSONObject(function.optString("arguments", "{}")) }.getOrDefault(JSONObject())
                val result = when (function.getString("name")) {
                    "search_entries" -> searchEntries(entries, args)
                    "get_entry" -> getEntry(entries, args)
                    else -> JSONObject().put("error", "unknown tool")
                }
                messages.put(
                    JSONObject()
                        .put("role", "tool")
                        .put("tool_call_id", call.getString("id"))
                        .put("content", result.toString()),
                )
            }
        }
        throw InsightException("翻了太多遍还没找到答案,换个问法试试")
    }

    // ── Tools ────────────────────────────────────────────────────────────────

    private fun searchEntries(entries: List<DiaryEntry>, args: JSONObject): JSONObject {
        val query = args.optString("query").trim()
        val from = args.optString("date_from").toDateOrNull()
        val to = args.optString("date_to").toDateOrNull()
        val tags = args.optJSONArray("tags")?.let { a -> List(a.length()) { a.getString(it) } }.orEmpty()
        val place = args.optString("place").trim()
        val limit = args.optInt("limit", 20).coerceIn(1, 40)

        val hits = entries.filter { entry ->
            (from == null || entry.date >= from) &&
                (to == null || entry.date <= to) &&
                (tags.isEmpty() || tags.any { t -> entry.tags.any { it.contains(t, ignoreCase = true) } }) &&
                (place.isEmpty() || entry.place?.contains(place, ignoreCase = true) == true) &&
                (query.isEmpty() || query.split(Regex("\\s+")).all { term ->
                    entry.text.contains(term, ignoreCase = true) ||
                        entry.tags.any { it.contains(term, ignoreCase = true) } ||
                        entry.place?.contains(term, ignoreCase = true) == true
                })
        }
        return JSONObject()
            .put("total", hits.size)
            .put("returned", minOf(hits.size, limit))
            .put("entries", JSONArray().also { array -> hits.take(limit).forEach { array.put(it.toJson(excerpt = 120)) } })
    }

    private fun getEntry(entries: List<DiaryEntry>, args: JSONObject): JSONObject {
        val id = args.optString("id")
        val entry = entries.firstOrNull { it.id == id }
            ?: return JSONObject().put("error", "no entry with id $id")
        return entry.toJson(excerpt = Int.MAX_VALUE)
    }

    private fun DiaryEntry.toJson(excerpt: Int) = JSONObject()
        .put("id", id)
        .put("date", date.toString())
        .put("weekday", WEEKDAYS[date.dayOfWeek.value - 1])
        .put("time", "%02d:%02d".format(createdAt.hour, createdAt.minute))
        .put("place", place ?: JSONObject.NULL)
        .put("tags", JSONArray(tags))
        .put("photos", photos.size)
        .put("text", if (text.length > excerpt) text.take(excerpt) + "…" else text)

    // ── Request shape ────────────────────────────────────────────────────────

    private fun request(messages: JSONArray) = JSONObject()
        .put("model", DeepSeekClient.MODEL)
        .put("thinking", JSONObject().put("type", "disabled"))
        .put("temperature", 0.3)
        .put("max_tokens", 800)
        .put("messages", messages)
        .put("tools", TOOLS)

    private fun message(role: String, content: String) = JSONObject().put("role", role).put("content", content)

    /** Strips `[[id]]` markers out of the text and keeps the ids that exist. */
    private fun parseAnswer(raw: String, entries: List<DiaryEntry>): ChatMessage {
        val ids = LinkedHashSet<String>()
        val known = entries.mapTo(HashSet()) { it.id }
        val text = REF.replace(raw) { m ->
            val id = m.groupValues[1].trim()
            if (id in known) ids.add(id)
            ""
        }
        val clean = DeepSeekClient.normalizePunctuation(text.trim())
            // Removing a marker leaves "…啤酒好喝 。" — pull the punctuation back in.
            .replace(Regex("[ \\t]+([，。；：！？」）])"), "$1")
            .replace(Regex("[ \\t]+\\n"), "\n")
            .replace(Regex("\\n{3,}"), "\n\n")
        if (clean.isEmpty()) throw InsightException("DeepSeek 什么也没说")
        return ChatMessage(fromUser = false, text = clean, refs = ids.toList())
    }

    private fun systemPrompt(today: LocalDate, entries: List<DiaryEntry>) = """
        你是一本私人日记 App 里的助手,帮用户在自己的日记里找东西、回答关于日记的问题。
        今天是 $today(${WEEKDAYS[today.dayOfWeek.value - 1]})。日记共 ${entries.size} 篇,
        最早一篇 ${entries.minOfOrNull { it.date } ?: "无"},最近一篇 ${entries.maxOfOrNull { it.date } ?: "无"}。

        规则:
        - 你看不到日记内容,必须先用 search_entries 查,需要全文再用 get_entry。不要凭空回答。
        - 查不到就直说「没找到」,不要编造。
        - 用简体中文,语气平和、简短,像熟悉这本日记的朋友。回答通常两三句,列举多篇时用换行分开。
        - 提到某篇日记时,在那句话末尾加引用标记 [[日记id]],id 用工具返回的 id 原样填写。用户点它就能打开那篇日记。
        - 相对时间(上次、最近、今年、上个月)按今天的日期换算成 date_from / date_to 再查。
        - 数数类的问题(几次、几篇)用 search_entries 返回的 total。
        - 不要用 Markdown 标题、粗体、列表符号,不用表情符号。中文标点用全角。
    """.trimIndent()

    companion object {
        const val NO_KEY = "no-key"
        private const val HISTORY_TURNS = 10
        private const val MAX_TOOL_ROUNDS = 6
        private val REF = Regex("\\[\\[([^\\]]+)]]")
        private val WEEKDAYS = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

        private val TOOLS: JSONArray by lazy {
            JSONArray()
                .put(
                    tool(
                        name = "search_entries",
                        description = "在日记里搜索。所有条件可选,同时给出时取交集。返回匹配总数和最多 limit 篇(每篇正文只给开头 120 字),按时间从新到旧。",
                        properties = JSONObject()
                            .put("query", prop("string", "关键词,匹配正文、标签、地点。多个词用空格分开,需同时出现。"))
                            .put("date_from", prop("string", "起始日期,YYYY-MM-DD,含当天"))
                            .put("date_to", prop("string", "结束日期,YYYY-MM-DD,含当天"))
                            .put("tags", JSONObject().put("type", "array").put("items", JSONObject().put("type", "string")).put("description", "标签,命中任意一个即可"))
                            .put("place", prop("string", "地点关键词"))
                            .put("limit", prop("integer", "最多返回几篇,默认 20,最大 40")),
                    ),
                )
                .put(
                    tool(
                        name = "get_entry",
                        description = "按 id 取一篇日记的完整内容。",
                        properties = JSONObject().put("id", prop("string", "日记 id")),
                        required = listOf("id"),
                    ),
                )
        }

        private fun tool(name: String, description: String, properties: JSONObject, required: List<String> = emptyList()) =
            JSONObject().put("type", "function").put(
                "function",
                JSONObject()
                    .put("name", name)
                    .put("description", description)
                    .put(
                        "parameters",
                        JSONObject()
                            .put("type", "object")
                            .put("properties", properties)
                            .put("required", JSONArray(required)),
                    ),
            )

        private fun prop(type: String, description: String) =
            JSONObject().put("type", type).put("description", description)

        private fun String.toDateOrNull(): LocalDate? =
            takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it.trim()) }.getOrNull() }
    }
}
