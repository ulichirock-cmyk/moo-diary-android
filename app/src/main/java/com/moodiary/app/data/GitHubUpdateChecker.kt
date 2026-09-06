package com.moodiary.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * The release feed: the latest GitHub Release of the app's own repository.
 *
 * The repo is public, so this is an anonymous call — no token is compiled into the APK.
 * The release the workflow publishes carries one asset (the signed APK) and a body with
 * one commit subject per line, which is exactly the shape 版本更新 wants to draw.
 *
 * Anything unexpected — no network, no releases yet, a release without an APK — reads as
 * "no update", because a failed check must never block the app it was launched with.
 */
class GitHubUpdateChecker(private val repo: String) : UpdateChecker {

    override suspend fun check(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        val release = runCatching { fetchLatestRelease() }.getOrNull() ?: return@withContext null
        val version = release.optString("tag_name").removePrefix("v").trim()
        if (version.isEmpty() || !isNewer(version, currentVersion)) return@withContext null

        val apk = release.optJSONArray("assets")?.let { assets ->
            (0 until assets.length())
                .map { assets.getJSONObject(it) }
                .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
        } ?: return@withContext null

        UpdateInfo(
            version = version,
            downloadSize = formatSize(apk.optLong("size")),
            notes = release.optString("body").lines()
                .map { it.trim().removePrefix("-").removePrefix("*").trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .take(6),
            downloadUrl = apk.optString("browser_download_url").takeIf { it.isNotBlank() }
                ?: return@withContext null,
        )
    }

    private fun fetchLatestRelease(): JSONObject? {
        val connection = (URL("https://api.github.com/repos/$repo/releases/latest")
            .openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        return try {
            if (connection.responseCode !in 200..299) return null
            JSONObject(connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
        } catch (e: IOException) {
            null
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        /**
         * `0.2.0` vs `0.10.1`, segment by segment — a plain string compare would call
         * 0.9.0 newer than 0.10.0. A segment that is not a number stops the comparison,
         * which is the safe answer: no update.
         */
        fun isNewer(candidate: String, current: String): Boolean {
            val a = candidate.split('.').map { it.toIntOrNull() ?: return false }
            val b = current.split('.').map { it.toIntOrNull() ?: return false }
            for (i in 0 until maxOf(a.size, b.size)) {
                val left = a.getOrElse(i) { 0 }
                val right = b.getOrElse(i) { 0 }
                if (left != right) return left > right
            }
            return false
        }

        fun formatSize(bytes: Long): String = when {
            bytes <= 0 -> "未知大小"
            bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
            else -> "%.0f KB".format(bytes / 1024.0)
        }
    }
}
