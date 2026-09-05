package com.moodiary.app.data

import com.moodiary.app.data.DeepSeekClient.firstMessage
import org.json.JSONArray
import org.json.JSONObject

/**
 * 自动标签: after an entry is published, asks DeepSeek which of the diary's tags fit
 * it. Existing tags are strongly preferred so the vocabulary stays small and the
 * 常用标签 list on 04 搜索 keeps meaning something; at most one new tag may be coined.
 * Returns an empty list on any failure — tagging is a nicety, never worth an error.
 */
class TagSuggester(private val apiKey: () -> String?) {

    suspend fun suggest(text: String, existingTags: List<String>, vocabulary: List<String>): List<String> {
        val key = apiKey()?.trim().orEmpty()
        if (key.isEmpty() || text.isBlank()) return emptyList()

        val body = JSONObject()
            .put("model", DeepSeekClient.MODEL)
            .put("thinking", JSONObject().put("type", "disabled"))
            .put("temperature", 0.2)
            .put("max_tokens", 100)
            .put("response_format", JSONObject().put("type", "json_object"))
            .put(
                "messages",
                JSONArray()
                    .put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
                    .put(JSONObject().put("role", "user").put("content", userPrompt(text, existingTags, vocabulary))),
            )

        return runCatching {
            val content = DeepSeekClient.chat(key, body).firstMessage().optString("content")
            val array = JSONObject(content).optJSONArray("tags") ?: JSONArray()
            List(array.length()) { array.optString(it) }
                .map { it.trim().removePrefix("#") }
                .filter { it.isNotEmpty() && it.length <= 6 }
                .distinct()
                .take(MAX_TAGS)
        }.getOrDefault(emptyList())
    }

    private fun userPrompt(text: String, existingTags: List<String>, vocabulary: List<String>) = buildString {
        append("已有标签库(优先从这里选):").append(vocabulary.joinToString("、")).append('\n')
        if (existingTags.isNotEmpty()) {
            append("用户已经给这篇打了:").append(existingTags.joinToString("、")).append(",不要重复给。\n")
        }
        append("\n日记正文:\n").append(text.trim())
    }

    private companion object {
        const val MAX_TAGS = 3
        val SYSTEM_PROMPT = """
            你给一本私人日记里的一篇日记打标签。
            - 从标签库里挑最贴切的 1 到 3 个;实在没有合适的,最多新造 1 个,2 到 4 个汉字,像标签库里那种风格(名词或场景词,不带#)。
            - 只根据正文里明确写到的内容打,不要推测。
            - 宁少勿多,一篇日记通常 1 到 2 个就够。
            - 只输出 JSON:{"tags":["...","..."]}
        """.trimIndent()
    }
}
