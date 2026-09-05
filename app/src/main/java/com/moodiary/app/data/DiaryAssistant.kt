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
                val result = DiaryTools.call(function.getString("name"), args, entries)
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
        private val WEEKDAYS = DiaryTools.WEEKDAYS

        /** [DiaryTools.SPECS] in the OpenAI function-calling shape DeepSeek expects. */
        private val TOOLS: JSONArray by lazy {
            JSONArray().also { array ->
                DiaryTools.SPECS
                    .filter { it.name != DiaryTools.OVERVIEW } // the system prompt already says this
                    .forEach { spec ->
                        array.put(
                            JSONObject().put("type", "function").put(
                                "function",
                                JSONObject()
                                    .put("name", spec.name)
                                    .put("description", spec.description)
                                    .put("parameters", spec.schema),
                            ),
                        )
                    }
            }
        }

    }
}
