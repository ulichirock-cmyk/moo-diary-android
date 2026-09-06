package com.moodiary.app.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Copies picked photos into the app's own files directory.
 *
 * The photo picker hands back `content://` URIs whose read grant lasts only for this
 * process, and they cannot be made persistable — so a URI written to the database is
 * dead on the next launch. Copying once at pick time and storing a `file://` URI is
 * what makes a photo survive. Seed photos are plain `https://` URLs and pass through.
 */
class PhotoStore(context: Context) {
    private val dir = File(context.filesDir, "photos").apply { mkdirs() }
    private val resolver = context.contentResolver

    /** Copies each pickable URI into [dir]; returns the `file://` URIs to store. */
    suspend fun import(uris: List<String>): List<String> = withContext(Dispatchers.IO) {
        uris.mapNotNull { uri ->
            if (!uri.startsWith("content://")) return@mapNotNull uri
            runCatching {
                val target = File(dir, "${System.currentTimeMillis()}-${uri.hashCode().toUInt()}.jpg")
                resolver.openInputStream(Uri.parse(uri))?.use { input ->
                    target.outputStream().use { input.copyTo(it) }
                } ?: return@mapNotNull null
                Uri.fromFile(target).toString()
            }.getOrNull()
        }
    }

    /** 恢复出厂设置: every copied photo goes. */
    fun clear() {
        dir.listFiles()?.forEach { it.delete() }
    }

    /** Deletes the copies behind [uris] that live in [dir]; remote and foreign URIs are left alone. */
    fun delete(uris: Collection<String>) {
        uris.forEach { uri ->
            if (uri.startsWith("file://")) {
                val file = File(Uri.parse(uri).path ?: return@forEach)
                if (file.parentFile == dir) file.delete()
            }
        }
    }
}
