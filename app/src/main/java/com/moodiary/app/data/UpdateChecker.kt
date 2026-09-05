package com.moodiary.app.data

/** What 版本更新 shows when a newer build exists. */
data class UpdateInfo(
    val version: String,
    val downloadSize: String,
    val notes: List<String>,
)

/**
 * Where the update banner's data comes from.
 *
 * There is no release endpoint for this app yet, so [StubUpdateChecker] returns the
 * sample release from the design. Point this at a real feed (or the store) and both
 * 我的 → 检查更新 and screen 12 follow automatically.
 */
interface UpdateChecker {
    /** Null when the installed build is current. */
    suspend fun check(currentVersion: String): UpdateInfo?
}

object StubUpdateChecker : UpdateChecker {
    override suspend fun check(currentVersion: String) = UpdateInfo(
        version = "1.0.0",
        downloadSize = "18.4 MB",
        notes = listOf(
            "日记可以记录地点了，支持地图选点",
            "洞察页可以直接向日记提问",
            "修复日历偶尔漏显标记的问题",
        ),
    )
}
