package com.watchmusic.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri

/**
 * Tags embedded in the audio file itself (ID3 for mp3, Vorbis comments for ogg/flac, ...).
 *
 * MediaStore usually derives its columns from these tags, but not every scanner fills them
 * in and SAF folder trees often expose nothing but a display name, so the app reads the
 * tags from the file when the metadata it has looks like a bare file name.
 */
data class EmbeddedMetadata(
    val title: String?,
    val artist: String?,
    val album: String?,
    val durationMs: Long,
    val trackNumber: Int,
    val year: Int,
)

object EmbeddedMetadataReader {

    fun read(context: Context, uri: Uri): EmbeddedMetadata? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            EmbeddedMetadata(
                title = TagText.clean(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)),
                artist = TagText.clean(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)),
                album = TagText.clean(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)),
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.trim()?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L,
                trackNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                    ?.substringBefore('/')?.trim()?.toIntOrNull() ?: 0,
                year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                    ?.trim()?.toIntOrNull() ?: 0,
            )
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
}

/** Small, unit-testable helpers for deciding which metadata value to show. */
object TagText {

    private val PLACEHOLDERS = setOf("<unknown>", "unknown", "null")

    fun clean(value: String?): String? {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        if (trimmed.lowercase() in PLACEHOLDERS) return null
        return trimmed
    }

    fun withoutExtension(fileName: String?): String? =
        clean(fileName)?.substringBeforeLast('.', clean(fileName).orEmpty())

    /**
     * True when [value] is missing, a placeholder, or just the file name (some scanners copy
     * the file name into the title column instead of parsing the embedded tag).
     */
    fun looksUntagged(value: String?, fileName: String?): Boolean {
        val cleaned = clean(value) ?: return true
        val name = withoutExtension(fileName)
        return name != null && cleaned.equals(name, ignoreCase = true)
    }
}
