package com.watchmusic.data.model

import android.net.Uri

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val trackNumber: Int,
    val year: Int,
    val uri: Uri,
) {
    /** Stable-ish seed used to build a colorful placeholder when no artwork exists. */
    val seed: Int get() = (title + "|" + artist + "|" + album).hashCode()
}

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val trackCount: Int,
    val coverTrack: Track?,
)

data class Artist(
    val name: String,
    val trackCount: Int,
    val albumCount: Int,
    val coverTrack: Track?,
)

data class Playlist(
    val id: String,
    val name: String,
    val createdAt: Long,
    val trackIds: List<String>,
)

data class Library(
    val tracks: List<Track>,
    val albums: List<Album>,
    val artists: List<Artist>,
) {
    val byId: Map<String, Track> = tracks.associateBy { it.id }

    fun tracksOf(albumId: Long): List<Track> =
        tracks.filter { it.albumId == albumId }.sortedBy { it.trackNumber }

    fun tracksBy(artist: String): List<Track> = tracks.filter { it.artist == artist }

    fun resolve(ids: List<String>): List<Track> = ids.mapNotNull { byId[it] }

    companion object {
        val EMPTY = Library(emptyList(), emptyList(), emptyList())
    }
}
