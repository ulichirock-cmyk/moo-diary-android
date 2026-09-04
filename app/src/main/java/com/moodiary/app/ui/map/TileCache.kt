package com.moodiary.app.ui.map

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.moodiary.app.util.appVersionName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** One raster tile: zoom level plus its column and row at that level. */
internal data class TileKey(val z: Int, val x: Int, val y: Int)

/**
 * Fetches map tiles and keeps them around.
 *
 * Coil is already in the project but is deliberately not used here: OpenStreetMap's
 * tile policy rejects generic library User-Agents (an okhttp default gets a "please
 * identify your app" placeholder image back with a 200, which would show up as an
 * unreadable map rather than an error), and tiles want their own small disk cache
 * keyed by z/x/y. Sixty lines of HttpURLConnection is less machinery than bending an
 * ImageLoader into the same shape.
 *
 * Parallelism is capped at two in-flight downloads, which is what the tile usage
 * policy asks of a client.
 */
internal class TileCache(context: Context) {

    private val memory = LruCache<TileKey, ImageBitmap>(140)
    private val downloads = Semaphore(2)
    private val userAgent = "Moodiary/${context.appVersionName()} (Android; personal diary app)"
    private val dir = File(context.cacheDir, "map-tiles")

    /** Already decoded, so a redraw can use it without touching a coroutine. */
    fun cached(key: TileKey): ImageBitmap? = memory.get(key)

    suspend fun load(key: TileKey): ImageBitmap? {
        memory.get(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            val file = File(dir, "${key.z}_${key.x}_${key.y}.png")
            val bytes = file.takeIf { it.isFile }?.let { runCatching { it.readBytes() }.getOrNull() }
                ?: downloads.withPermit { download(key) }?.also { save(file, it) }
                ?: return@withContext null
            val bitmap = runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull()
                ?: return@withContext null
            bitmap.asImageBitmap().also { memory.put(key, it) }
        }
    }

    private fun download(key: TileKey): ByteArray? = runCatching {
        val url = URL("https://tile.openstreetmap.org/${key.z}/${key.x}/${key.y}.png")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            setRequestProperty("User-Agent", userAgent)
            connectTimeout = 10_000
            readTimeout = 15_000
        }
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }.getOrNull()

    private fun save(file: File, bytes: ByteArray) {
        runCatching {
            dir.mkdirs()
            file.writeAtomically(bytes)
            trim()
        }
    }

    /**
     * Keeps the on-disk cache from growing without bound. Tiles are ~30 KB each, so
     * 600 of them is under 20 MB; the oldest third goes when we cross the line.
     */
    private fun trim() {
        val files = dir.listFiles() ?: return
        if (files.size <= 600) return
        files.sortedBy { it.lastModified() }.take(files.size - 400).forEach { it.delete() }
    }
}

private fun File.writeAtomically(bytes: ByteArray) {
    val temp = File(parentFile, "$name.tmp")
    temp.writeBytes(bytes)
    if (!temp.renameTo(this)) temp.delete()
}
