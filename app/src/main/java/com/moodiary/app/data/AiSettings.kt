package com.moodiary.app.data

import android.content.Context
import com.moodiary.app.BuildConfig
import java.security.SecureRandom
import java.time.LocalDate
import java.util.Base64

/**
 * The DeepSeek API key. Two sources, user wins:
 *
 * 1. What the user typed into 我的 → AI 洞察, kept in SharedPreferences on this device.
 * 2. `DEEPSEEK_API_KEY` from local.properties, baked into [BuildConfig] as a developer
 *    default so a fresh debug build can generate reviews without touching the UI.
 */
class AiSettings(context: Context) {
    private val prefs = context.getSharedPreferences("ai", Context.MODE_PRIVATE)

    var apiKey: String?
        get() = prefs.getString(KEY_API_KEY, null)?.takeIf { it.isNotBlank() }
            ?: BuildConfig.DEEPSEEK_API_KEY.takeIf { it.isNotBlank() }
        set(value) {
            prefs.edit().putString(KEY_API_KEY, value?.trim()).apply()
        }

    /** 自动标签: tag a freshly published entry from its text. On by default; needs a key to do anything. */
    var autoTag: Boolean
        get() = prefs.getBoolean(KEY_AUTO_TAG, true)
        set(value) {
            prefs.edit().putBoolean(KEY_AUTO_TAG, value).apply()
        }

    /** 写作引导: the placeholder of a new entry is a question for today. On by default; works without a key via a canned list. */
    var writingPrompt: Boolean
        get() = prefs.getBoolean(KEY_WRITING_PROMPT, true)
        set(value) {
            prefs.edit().putBoolean(KEY_WRITING_PROMPT, value).apply()
        }

    /** The question generated for [date], or null when today's has not been fetched yet. */
    fun writingPromptFor(date: LocalDate): String? =
        prefs.getString(KEY_PROMPT_TEXT, null)
            ?.takeIf { prefs.getString(KEY_PROMPT_DATE, null) == date.toString() && it.isNotBlank() }

    fun saveWritingPrompt(date: LocalDate, text: String) {
        val history = recentWritingPrompts().filterNot { it == text }.take(RECENT_PROMPTS - 1)
        prefs.edit()
            .putString(KEY_PROMPT_DATE, date.toString())
            .putString(KEY_PROMPT_TEXT, text)
            .putString(KEY_PROMPT_HISTORY, (listOf(text) + history).joinToString("\n"))
            .apply()
    }

    /** The last few questions, newest first — sent along so the model does not repeat itself. */
    fun recentWritingPrompts(): List<String> =
        prefs.getString(KEY_PROMPT_HISTORY, null)?.split('\n')?.filter { it.isNotBlank() }.orEmpty()

    /** Claude Code 连接: keep [DiaryMcpServer] listening. Off by default; nothing is reachable until it is on. */
    var mcpEnabled: Boolean
        get() = prefs.getBoolean(KEY_MCP_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_MCP_ENABLED, value).apply()
        }

    /** The bearer token Claude Code must send. Made once, on first read; [resetMcpToken] makes a new one. */
    val mcpToken: String
        get() = prefs.getString(KEY_MCP_TOKEN, null)?.takeIf { it.isNotBlank() } ?: resetMcpToken()

    fun resetMcpToken(): String {
        val bytes = ByteArray(18).also { SecureRandom().nextBytes(it) }
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        prefs.edit().putString(KEY_MCP_TOKEN, token).apply()
        return token
    }

    /** True when the user set a key themselves (as opposed to the build-time default). */
    val hasUserKey: Boolean get() = !prefs.getString(KEY_API_KEY, null).isNullOrBlank()

    private companion object {
        const val KEY_API_KEY = "deepseek_api_key"
        const val KEY_AUTO_TAG = "auto_tag"
        const val KEY_WRITING_PROMPT = "writing_prompt"
        const val KEY_PROMPT_DATE = "writing_prompt_date"
        const val KEY_PROMPT_TEXT = "writing_prompt_text"
        const val KEY_PROMPT_HISTORY = "writing_prompt_history"
        const val RECENT_PROMPTS = 7
        const val KEY_MCP_ENABLED = "mcp_enabled"
        const val KEY_MCP_TOKEN = "mcp_token"
    }
}
