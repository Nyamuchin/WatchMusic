package com.watchmusic.data

import com.watchmusic.data.model.Album
import com.watchmusic.data.model.Artist
import com.watchmusic.data.model.Library
import com.watchmusic.data.model.Track

/**
 * Pure search over the scanned library. Kept free of Android and Compose APIs so
 * the matching rules are unit tested directly.
 */
object SearchFilter {

    fun normalize(query: String): String = query.trim()

    fun matches(fields: List<String>, query: String): Boolean {
        val needle = normalize(query)
        if (needle.isEmpty()) return false
        return fields.any { it.contains(needle, ignoreCase = true) }
    }

    /** Filters [items] by [query], keeping at most [limit] matches. */
    fun <T> filter(
        items: List<T>,
        query: String,
        limit: Int,
        fieldsOf: (T) -> List<String>,
    ): List<T> {
        if (normalize(query).isEmpty() || limit <= 0) return emptyList()
        return items.asSequence()
            .filter { matches(fieldsOf(it), query) }
            .take(limit)
            .toList()
    }

    fun tracks(library: Library, query: String, limit: Int = 30): List<Track> =
        filter(library.tracks, query, limit) { listOf(it.title, it.artist, it.album) }

    fun albums(library: Library, query: String, limit: Int = 12): List<Album> =
        filter(library.albums, query, limit) { listOf(it.title, it.artist) }

    fun artists(library: Library, query: String, limit: Int = 12): List<Artist> =
        filter(library.artists, query, limit) { listOf(it.name) }
}
