package com.watchmusic.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.watchmusic.WatchMusicApp
import com.watchmusic.data.model.Library
import com.watchmusic.data.model.Playlist
import com.watchmusic.data.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Library, permission, playlist and preference state for the browsing screens. */
class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as WatchMusicApp).container
    val prefs = container.prefs
    val artwork = container.artwork

    val library: StateFlow<Library?> = container.repository.library
    val loading: StateFlow<Boolean> = container.repository.loading
    val error: StateFlow<String?> = container.repository.error

    val favorites: StateFlow<List<String>> = prefs.favorites
    val playlists: StateFlow<List<Playlist>> = prefs.playlists
    val recent: StateFlow<List<String>> = prefs.recent
    val useArtworkColor: StateFlow<Boolean> = prefs.useArtworkColor
    val haptic: StateFlow<Boolean> = prefs.haptic
    val crownVolume: StateFlow<Boolean> = prefs.crownVolume
    val uiScale: StateFlow<Float> = prefs.uiScale
    val libraryFolder: StateFlow<String?> = prefs.libraryFolder

    private val _hasPermission = MutableStateFlow(container.repository.hasPermission())
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    init {
        refreshIfPermitted()
    }

    fun refreshIfPermitted() {
        val granted = container.repository.hasPermission()
        _hasPermission.value = granted
        if (granted) {
            viewModelScope.launch { container.repository.refresh() }
        }
    }

    fun refresh() {
        _hasPermission.value = container.repository.hasPermission()
        viewModelScope.launch { container.repository.refresh() }
    }

    fun toggleFavorite(track: Track) = prefs.toggleFavorite(track.id)

    fun isFavorite(track: Track): Boolean = prefs.isFavorite(track.id)

    fun createPlaylist(name: String): Playlist = prefs.createPlaylist(name)

    fun renamePlaylist(id: String, name: String) = prefs.renamePlaylist(id, name)

    fun deletePlaylist(id: String) = prefs.deletePlaylist(id)

    fun addTracksToPlaylist(id: String, trackIds: List<String>) = prefs.addTracksToPlaylist(id, trackIds)

    fun removeTrackFromPlaylist(id: String, trackId: String) = prefs.removeTrackFromPlaylist(id, trackId)

    fun setUseArtworkColor(value: Boolean) = prefs.setUseArtworkColor(value)

    fun setHaptic(value: Boolean) = prefs.setHaptic(value)

    fun setCrownVolume(value: Boolean) = prefs.setCrownVolume(value)

    fun setUiScale(value: Float) = prefs.setUiScale(value)

    /** Restricts the library to a SAF folder tree, or null to read the whole device library. */
    fun setLibraryFolder(treeUri: String?) {
        prefs.setLibraryFolder(treeUri)
        refresh()
    }
}
