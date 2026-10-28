package com.watchmusic.data

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import com.watchmusic.data.model.Track
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads cover art straight from MediaStore. Small custom cache instead of a full
 * image loading library: the app only ever needs one or two covers at a time.
 */
class ArtworkLoader(private val context: Context) {

    private val cache = object : LruCache<String, Bitmap>(CACHE_KB) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    suspend fun load(track: Track, sizePx: Int = 320): Bitmap? = withContext(Dispatchers.IO) {
        val key = "${track.id}@$sizePx"
        cache.get(key)?.let { return@withContext it }
        val bitmap = loadThumbnail(track, sizePx)
            ?: loadLegacyAlbumArt(track, sizePx)
            ?: loadEmbeddedArt(track, sizePx)
        if (bitmap != null) cache.put(key, bitmap)
        bitmap
    }

    /** JPEG bytes handed to the media session so notifications / lock screen show the cover. */
    suspend fun loadNotificationArt(track: Track): ByteArray? = withContext(Dispatchers.IO) {
        val bitmap = load(track, sizePx = 256) ?: return@withContext null
        ByteArrayOutputStream().use { output ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 82, output)) return@withContext null
            output.toByteArray()
        }
    }

    private fun loadThumbnail(track: Track, sizePx: Int): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return runCatching {
            context.contentResolver.loadThumbnail(track.uri, Size(sizePx, sizePx), null)
        }.getOrNull()
    }

    private fun loadLegacyAlbumArt(track: Track, sizePx: Int): Bitmap? {
        if (track.albumId <= 0L) return null
        val uri = ContentUris.withAppendedId(LEGACY_ALBUM_ART_URI, track.albumId)
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrNull()?.let { scaleDown(it, sizePx) }
    }

    private fun loadEmbeddedArt(track: Track, sizePx: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, track.uri)
            val bytes = retriever.embeddedPicture ?: return null
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { scaleDown(it, sizePx) }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun scaleDown(bitmap: Bitmap, sizePx: Int): Bitmap {
        val largest = maxOf(bitmap.width, bitmap.height)
        if (largest <= sizePx) return bitmap
        val ratio = sizePx.toFloat() / largest
        val width = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val height = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private companion object {
        const val CACHE_KB = 16 * 1024
        val LEGACY_ALBUM_ART_URI: Uri = Uri.parse("content://media/external/audio/albumart")
    }
}
