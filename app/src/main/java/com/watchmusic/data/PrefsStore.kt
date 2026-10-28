package com.watchmusic.data

import android.content.Context
import com.watchmusic.data.model.Playlist
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

/**
 * SharedPreferences backed app state. Everything is mirrored in a StateFlow so
 * Compose and the media service observe the same source of truth.
 */
class PrefsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private val _favorites = MutableStateFlow(MediaJson.decodeIds(prefs.getString(KEY_FAVORITES, null)))
    val favorites: StateFlow<List<String>> = _favorites.asStateFlow()

    private val _playlists = MutableStateFlow(MediaJson.decodePlaylists(prefs.getString(KEY_PLAYLISTS, null)))
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _recent = MutableStateFlow(MediaJson.decodeIds(prefs.getString(KEY_RECENT, null)))
    val recent: StateFlow<List<String>> = _recent.asStateFlow()

    private val _useArtworkColor = MutableStateFlow(prefs.getBoolean(KEY_ARTWORK_COLOR, true))
    val useArtworkColor: StateFlow<Boolean> = _useArtworkColor.asStateFlow()

    private val _haptic = MutableStateFlow(prefs.getBoolean(KEY_HAPTIC, true))
    val haptic: StateFlow<Boolean> = _haptic.asStateFlow()

    private val _crownVolume = MutableStateFlow(prefs.getBoolean(KEY_CROWN_VOLUME, true))
    val crownVolume: StateFlow<Boolean> = _crownVolume.asStateFlow()

    private val _uiScale = MutableStateFlow(snapScale(prefs.getFloat(KEY_UI_SCALE, 1f)))
    val uiScale: StateFlow<Float> = _uiScale.asStateFlow()

    private val _libraryFolder = MutableStateFlow(
        prefs.getString(KEY_LIBRARY_FOLDER, null)?.takeIf { it.isNotBlank() },
    )

    /** SAF tree uri the library is restricted to, or null to read the whole device library. */
    val libraryFolder: StateFlow<String?> = _libraryFolder.asStateFlow()

    // ---------------------------------------------------------------- favorites

    fun isFavorite(trackId: String): Boolean = trackId in _favorites.value

    fun toggleFavorite(trackId: String) {
        val current = _favorites.value
        val next = if (trackId in current) current - trackId else listOf(trackId) + current
        _favorites.value = next
        prefs.edit().putString(KEY_FAVORITES, MediaJson.encodeIds(next)).apply()
    }

    // ---------------------------------------------------------------- playlists

    fun createPlaylist(name: String): Playlist {
        val playlist = Playlist(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifBlank { "未命名歌单" },
            createdAt = System.currentTimeMillis(),
            trackIds = emptyList(),
        )
        savePlaylists(_playlists.value + playlist)
        return playlist
    }

    fun renamePlaylist(playlistId: String, name: String) {
        val clean = name.trim()
        if (clean.isBlank()) return
        savePlaylists(_playlists.value.map { if (it.id == playlistId) it.copy(name = clean) else it })
    }

    fun deletePlaylist(playlistId: String) {
        savePlaylists(_playlists.value.filterNot { it.id == playlistId })
    }

    fun addTracksToPlaylist(playlistId: String, trackIds: List<String>) {
        if (trackIds.isEmpty()) return
        savePlaylists(
            _playlists.value.map { playlist ->
                if (playlist.id == playlistId) {
                    playlist.copy(trackIds = (playlist.trackIds + trackIds).distinct())
                } else {
                    playlist
                }
            },
        )
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        savePlaylists(
            _playlists.value.map { playlist ->
                if (playlist.id == playlistId) {
                    playlist.copy(trackIds = playlist.trackIds - trackId)
                } else {
                    playlist
                }
            },
        )
    }

    private fun savePlaylists(playlists: List<Playlist>) {
        _playlists.value = playlists
        prefs.edit().putString(KEY_PLAYLISTS, MediaJson.encodePlaylists(playlists)).apply()
    }

    // ------------------------------------------------------------ recent plays

    fun recordPlayed(trackId: String) {
        val next = (listOf(trackId) + _recent.value.filterNot { it == trackId }).take(MAX_RECENT)
        _recent.value = next
        prefs.edit().putString(KEY_RECENT, MediaJson.encodeIds(next)).apply()
    }

    // ---------------------------------------------------------------- settings

    fun setUseArtworkColor(enabled: Boolean) {
        _useArtworkColor.value = enabled
        prefs.edit().putBoolean(KEY_ARTWORK_COLOR, enabled).apply()
    }

    fun setHaptic(enabled: Boolean) {
        _haptic.value = enabled
        prefs.edit().putBoolean(KEY_HAPTIC, enabled).apply()
    }

    fun setCrownVolume(enabled: Boolean) {
        _crownVolume.value = enabled
        prefs.edit().putBoolean(KEY_CROWN_VOLUME, enabled).apply()
    }

    fun setUiScale(scale: Float) {
        val snapped = snapScale(scale)
        _uiScale.value = snapped
        prefs.edit().putFloat(KEY_UI_SCALE, snapped).apply()
    }

    /** Snaps to the closest preset step (25%) so the chips always show a selection. */
    private fun snapScale(value: Float): Float {
        val snapped = (value * UI_SCALE_STEPS).roundToInt() / UI_SCALE_STEPS.toFloat()
        return snapped.coerceIn(MIN_UI_SCALE, MAX_UI_SCALE)
    }

    fun setLibraryFolder(treeUri: String?) {
        val clean = treeUri?.takeIf { it.isNotBlank() }
        _libraryFolder.value = clean
        prefs.edit().putString(KEY_LIBRARY_FOLDER, clean).apply()
    }

    // ---------------------------------------------------------------- playback

    fun savePlaybackState(trackId: String?, positionMs: Long) {
        prefs.edit()
            .putString(KEY_LAST_TRACK, trackId)
            .putLong(KEY_LAST_POSITION, positionMs)
            .apply()
    }

    fun lastTrackId(): String? = prefs.getString(KEY_LAST_TRACK, null)

    fun lastPositionMs(): Long = prefs.getLong(KEY_LAST_POSITION, 0L)

    /** Persists the whole play queue (media ids in play order) plus the current index. */
    fun saveQueue(ids: List<String>, index: Int) {
        prefs.edit()
            .putString(KEY_QUEUE, MediaJson.encodeIds(ids))
            .putInt(KEY_QUEUE_INDEX, index)
            .apply()
    }

    fun queueIds(): List<String> =
        MediaJson.decodeIds(prefs.getString(KEY_QUEUE, null))

    fun queueIndex(): Int = prefs.getInt(KEY_QUEUE_INDEX, 0)

    fun saveShuffle(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHUFFLE, enabled).apply()
    }

    fun shuffle(): Boolean = prefs.getBoolean(KEY_SHUFFLE, false)

    fun saveRepeatMode(mode: Int) {
        prefs.edit().putInt(KEY_REPEAT, mode).apply()
    }

    fun repeatMode(): Int = prefs.getInt(KEY_REPEAT, 0)

    private companion object {
        const val FILE = "watchmusic"
        const val KEY_FAVORITES = "favorites"
        const val KEY_PLAYLISTS = "playlists"
        const val KEY_RECENT = "recent"
        const val KEY_ARTWORK_COLOR = "use_artwork_color"
        const val KEY_HAPTIC = "haptic"
        const val KEY_CROWN_VOLUME = "crown_volume"
        const val KEY_UI_SCALE = "ui_scale"
        const val KEY_LIBRARY_FOLDER = "library_folder"
        const val KEY_LAST_TRACK = "last_track_id"
        const val KEY_LAST_POSITION = "last_position_ms"
        const val KEY_QUEUE = "queue_ids"
        const val KEY_QUEUE_INDEX = "queue_index"
        const val KEY_SHUFFLE = "shuffle"
        const val KEY_REPEAT = "repeat_mode"
        const val MAX_RECENT = 30
        const val MIN_UI_SCALE = 0.25f
        const val MAX_UI_SCALE = 2f
        const val UI_SCALE_STEPS = 4
    }
}
