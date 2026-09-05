package com.moodiary.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * The read-only tools a model gets over the diary — the same three whether the caller
 * is 问问日记 (DeepSeek, in-process) or Claude Code (over MCP, see [DiaryMcpServer]).
 * One implementation so both see identical results; the diary text never leaves the
 * device except through these.
 */
object DiaryTools {
    const val SEARCH = "search_entries"
    const val GET = "get_entry"
    const val OVERVIEW = "diary_overview"

    /** One tool: its name, what it does, and a JSON-Schema object for its arguments. */
    class Spec(val name: String, val description: String, val schema: JSONObject)

    val SPECS: List<Spec> by lazy {
        listOf(
            Spec(
                SEARCH,
                "在日记里搜索。所有条件可选,同时给出时取交集。返回匹配总数和最多 limit 篇(每篇正文只给开头 120 字),按时间从新到旧。",
                schema(
                    JSONObject()
                        .put("query", prop("string", "关键词,匹配正文、标签、地点。多个词用空格分开,需同时出现。"))
                        .put("date_from", prop("string", "起始日期,YYYY-MM-DD,含当天"))
                        .put("date_to", prop("string", "结束日期,YYYY-MM-DD,含当天"))
                        .put("tags", JSONObject().put("type", "array").put("items", JSONObject().put("type", "string")).put("description", "标签,命中任意一个即可"))
                        .put("place", prop("string", "地点关键词"))
                        .put("limit", prop("integer", "最多返回几篇,默认 20,最大 40")),
                ),
            ),
            Spec(
                GET,
                "按 id 取一篇日记的完整内容。",
                schema(JSONObject().put("id", prop("string", "日记 id")), required = listOf("id")),
            ),
            Spec(
                OVERVIEW,
                "这本日记的概况:篇数、时间跨度、所有标签和地点各出现几次。适合先调一次再决定怎么搜。",
                schema(JSONObject()),
            ),
        )
    }

    /** Runs [name] with [args]; unknown names come back as `{"error": ...}`. */
    fun call(name: String, args: JSONObject, entries: List<DiaryEntry>): JSONObject = when (name) {
        SEARCH -> search(entries, args)
        GET -> get(entries, args)
        OVERVIEW -> overview(entries)
        else -> JSONObject().put("error", "unknown tool $name")
    }

    fun search(entries: List<DiaryEntry>, args: JSONObject): JSONObject {
        val query = args.optString("query").trim()
        val from = args.optString("date_from").toDateOrNull()
        val to = args.optString("date_to").toDateOrNull()
        val tags = args.optJSONArray("tags")?.let { a -> List(a.length()) { a.optString(it) } }.orEmpty()
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
        }.sortedByDescending { it.createdAt }
        return JSONObject()
            .put("total", hits.size)
            .put("returned", minOf(hits.size, limit))
            .put("entries", JSONArray().also { array -> hits.take(limit).forEach { array.put(it.toJson(excerpt = 120)) } })
    }

    fun get(entries: List<DiaryEntry>, args: JSONObject): JSONObject {
        val id = args.optString("id")
        val entry = entries.firstOrNull { it.id == id }
            ?: return JSONObject().put("error", "no entry with id $id")
        return entry.toJson(excerpt = Int.MAX_VALUE)
    }

    fun overview(entries: List<DiaryEntry>): JSONObject = JSONObject()
        .put("total", entries.size)
        .put("first", entries.minOfOrNull { it.date }?.toString() ?: JSONObject.NULL)
        .put("last", entries.maxOfOrNull { it.date }?.toString() ?: JSONObject.NULL)
        .put("photos", entries.photoCount())
        .put("tags", JSONObject().also { o -> entries.tagCounts().forEach { (t, n) -> o.put(t, n) } })
        .put("places", JSONObject().also { o -> entries.placeCounts().forEach { (p, n) -> o.put(p, n) } })

    fun DiaryEntry.toJson(excerpt: Int): JSONObject = JSONObject()
        .put("id", id)
        .put("date", date.toString())
        .put("weekday", WEEKDAYS[date.dayOfWeek.value - 1])
        .put("time", "%02d:%02d".format(createdAt.hour, createdAt.minute))
        .put("place", place ?: JSONObject.NULL)
        .put("tags", JSONArray(tags))
        .put("photos", photos.size)
        .put("text", if (text.length > excerpt) text.take(excerpt) + "…" else text)

    val WEEKDAYS = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

    private fun schema(properties: JSONObject, required: List<String> = emptyList()) = JSONObject()
        .put("type", "object")
        .put("properties", properties)
        .put("required", JSONArray(required))

    private fun prop(type: String, description: String) =
        JSONObject().put("type", type).put("description", description)

    private fun String.toDateOrNull(): LocalDate? =
        takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it.trim()) }.getOrNull() }
}
