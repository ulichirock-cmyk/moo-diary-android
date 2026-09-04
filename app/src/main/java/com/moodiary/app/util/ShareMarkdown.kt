package com.moodiary.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Writes the Markdown export to the app's cache and hands it to the system share
 * sheet. Kept out of the UI so a future "save to Documents" path can reuse it.
 */
fun Context.shareMarkdown(markdown: String, fileName: String = "moodiary.md") {
    val dir = File(cacheDir, "exports").apply { mkdirs() }
    val file = File(dir, fileName).apply { writeText(markdown) }
    val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/markdown"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TITLE, fileName)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
