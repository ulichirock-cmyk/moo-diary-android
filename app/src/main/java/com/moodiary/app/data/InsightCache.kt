package com.moodiary.app.data

import android.content.Context

/**
 * The last generated paragraph for each [ReviewPeriod], kept on disk so reopening the
 * app shows the reviews at once instead of paying for three DeepSeek calls.
 *
 * A cached text is only reused while its [Cached.fingerprint] — a digest of the entries
 * it was written from — still matches; the moment an entry in that span changes, the
 * view model regenerates and overwrites it.
 */
class InsightCache(context: Context) {
    private val prefs = context.getSharedPreferences("insights", Context.MODE_PRIVATE)

    class Cached(val fingerprint: String, val text: String)

    operator fun get(period: ReviewPeriod): Cached? {
        val fingerprint = prefs.getString(key(period, "fp"), null) ?: return null
        val text = prefs.getString(key(period, "text"), null) ?: return null
        return Cached(fingerprint, text)
    }

    fun put(period: ReviewPeriod, fingerprint: String, text: String) {
        prefs.edit()
            .putString(key(period, "fp"), fingerprint)
            .putString(key(period, "text"), text)
            .apply()
    }

    private fun key(period: ReviewPeriod, field: String) = "${period.name.lowercase()}_$field"
}
