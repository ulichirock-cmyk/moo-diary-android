package com.moodiary.app.data

/** What 版本更新 shows when a newer build exists. */
data class UpdateInfo(
    val version: String,
    val downloadSize: String,
    val notes: List<String>,
    /** Where the APK is; null in the sample data used by previews. */
    val downloadUrl: String? = null,
)

/**
 * Where the update banner's data comes from — [GitHubUpdateChecker] in the app,
 * [StubUpdateChecker] (the design's sample release) in previews.
 */
interface UpdateChecker {
    /** Null when the installed build is current. */
    suspend fun check(currentVersion: String): UpdateInfo?
}

/** The design's sample release, for previews and for a build with no repo configured. */
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
