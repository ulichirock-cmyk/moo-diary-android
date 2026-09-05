package com.moodiary.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** A user-facing failure from the AI layer; [message] is ready to show. */
class InsightException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * One POST to DeepSeek's OpenAI-compatible chat endpoint over plain HttpURLConnection —
 * the same choice [com.moodiary.app.ui.map.TileCache] makes: a single request is less
 * machinery than an HTTP client dependency. Shared by the review generator and the
 * diary assistant.
 */
object DeepSeekClient {
    const val MODEL = "deepseek-v4-flash"
    private const val ENDPOINT = "https://api.deepseek.com/chat/completions"

    /** Sends [body] and returns the parsed response. Throws [InsightException] on any failure. */
    suspend fun chat(apiKey: String, body: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 90_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Authorization", "Bearer $apiKey")
        }
        try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw InsightException(describeHttpError(status, text))
            runCatching { JSONObject(text) }
                .getOrElse { throw InsightException("DeepSeek 返回了看不懂的内容", it) }
        } catch (e: IOException) {
            throw InsightException("网络不通,稍后再试", e)
        } finally {
            connection.disconnect()
        }
    }

    /** `choices[0].message` of a response. */
    fun JSONObject.firstMessage(): JSONObject =
        runCatching { getJSONArray("choices").getJSONObject(0).getJSONObject("message") }
            .getOrElse { throw InsightException("DeepSeek 返回了看不懂的内容", it) }

    private fun describeHttpError(status: Int, body: String): String {
        val serverMessage = runCatching {
            JSONObject(body).getJSONObject("error").getString("message")
        }.getOrNull()
        return when (status) {
            401 -> "API Key 无效,去「我的 → AI 洞察」检查一下"
            402 -> "DeepSeek 账户余额不足"
            429 -> "请求太频繁,等一会儿再试"
            else -> serverMessage?.let { "DeepSeek 出错($status):$it" } ?: "DeepSeek 出错($status)"
        }
    }

    /**
     * The model mixes half- and full-width punctuation from one run to the next. The
     * design's copy is full-width throughout, with 「」 for quoted words, so bring the
     * reply in line. Digits are left alone (1.5, 08:14).
     */
    fun normalizePunctuation(text: String): String {
        val sb = StringBuilder(text.length)
        for (i in text.indices) {
            val c = text[i]
            val prev = text.getOrNull(i - 1)
            val next = text.getOrNull(i + 1)
            val betweenDigits = prev?.isDigit() == true && next?.isDigit() == true
            sb.append(
                when {
                    c == ',' && !betweenDigits -> '，'
                    c == '.' && !betweenDigits && prev?.isDigit() != true -> '。'
                    c == ';' -> '；'
                    c == ':' && !betweenDigits -> '：'
                    c == '!' -> '！'
                    c == '?' -> '？'
                    c == '(' -> '（'
                    c == ')' -> '）'
                    c == '“' -> '「'
                    c == '”' -> '」'
                    else -> c
                },
            )
        }
        return sb.toString().replace("， ", "，").replace("。 ", "。").replace("： ", "：")
    }
}
