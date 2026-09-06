package com.moodiary.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads the APK from the release and hands it to the system installer.
 *
 * Android gives no third-party app a silent install: the most this can do is put the
 * file in front of the package installer and let the user confirm. Before that the user
 * has to have allowed 未知来源 for moodiary once — [canInstall] says whether they have,
 * and [openInstallPermissionSettings] is where to send them if not.
 */
class UpdateInstaller(private val context: Context) {

    private val dir = File(context.cacheDir, "updates")

    /** Downloads to the cache, reporting 0f..1f. Returns the file, or throws [IllegalStateException]. */
    suspend fun download(url: String, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        dir.mkdirs()
        // One update at a time: the previous half-finished download is worth nothing.
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, "moodiary-update.apk")

        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 60_000
            instanceFollowRedirects = true
        }
        try {
            if (connection.responseCode !in 200..299) {
                error("下载失败(HTTP ${connection.responseCode})")
            }
            val total = connection.contentLengthLong
            var read = 0L
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        read += n
                        if (total > 0) onProgress((read.toDouble() / total).toFloat())
                    }
                }
            }
            onProgress(1f)
            target
        } finally {
            connection.disconnect()
        }
    }

    /** True once the user has allowed this app to install packages. */
    fun canInstall(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** Opens the system installer on [apk]. */
    fun install(apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
