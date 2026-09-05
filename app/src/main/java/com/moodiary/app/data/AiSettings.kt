package com.moodiary.app.data

import android.content.Context
import com.moodiary.app.BuildConfig

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

    /** True when the user set a key themselves (as opposed to the build-time default). */
    val hasUserKey: Boolean get() = !prefs.getString(KEY_API_KEY, null).isNullOrBlank()

    private companion object {
        const val KEY_API_KEY = "deepseek_api_key"
    }
}
