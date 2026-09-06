package com.moodiary.app.data

import android.content.Context

/**
 * The one thing 我的 lets you change about yourself: the name over the stats.
 *
 * A single-user diary has no sign-in, so this is a string on this device and nothing
 * more. Blank falls back to [OWNER_NAME], the name the design wrote.
 */
class ProfileSettings(context: Context) {
    private val prefs = context.getSharedPreferences("profile", Context.MODE_PRIVATE)

    var ownerName: String
        get() = prefs.getString(KEY_OWNER_NAME, null)?.takeIf { it.isNotBlank() } ?: OWNER_NAME
        set(value) {
            prefs.edit()
                .putString(KEY_OWNER_NAME, value.trim().take(MAX_NAME_LENGTH).takeIf { it.isNotEmpty() })
                .apply()
        }

    /** 恢复出厂设置: back to the design's name. */
    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        /** Long enough for a full Chinese name or a handle; the avatar only shows the first character. */
        const val MAX_NAME_LENGTH = 12
        private const val KEY_OWNER_NAME = "owner_name"
    }
}
