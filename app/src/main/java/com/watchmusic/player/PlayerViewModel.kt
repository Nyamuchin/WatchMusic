package com.watchmusic.player

import android.app.Application
import android.content.ComponentName
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.watchmusic.WatchMusicApp
import com.watchmusic.data.model.Track
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Owns the MediaController connection and exposes one flow for the whole UI.
 * All mutations happen on the main thread, as required by Media3 controllers.
 */
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as WatchMusicApp).container
    private val prefs = container.prefs
    private val repository = container.repository
    private val artworkLoader = container.artwork
    private val volumeController = container.volume

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private val _volumePercent = MutableStateFlow(volumeController.percent())
    val volumePercent: StateFlow<Int> = _volumePercent.asStateFlow()

    private val _volumeHudVisible = MutableStateFlow(false)
    val volumeHudVisible: StateFlow<Boolean> = _volumeHudVisible.asStateFlow()

    private val sleepTimer = SleepTimer {
        controller?.pause()
        _state.update { it.copy(isPlaying = false) }
    }
    val sleepRemainingMs: StateFlow<Long> = sleepTimer.remainingMs

    private var controller: MediaController? = null
    private var tickerJob: Job? = null
    private var hudJob: Job? = null

    /** Last queue signature written to preferences, used to avoid redundant writes. */
    private var savedQueueSignature: String? = null
    private var savedQueueIndex: Int = -1

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            refreshState()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val track = mediaItem?.mediaId?.let { repository.library.value?.byId?.get(it) }
            if (track != null) {
                prefs.recordPlayed(track.id)
                prefs.savePlaybackState(track.id, 0L)
            }
            refreshState()
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.update { it.copy(error = describe(error)) }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            // Pausing is the most common moment to leave the app, so store the
            // position and queue right away instead of waiting for the next tick.
            if (!isPlaying) {
                controller?.let { player ->
                    persistPosition(player)
                    persistQueue(player)
                }
            }
            refreshState()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY && _state.value.error != null) {
                _state.update { it.copy(error = null) }
            }
        }
    }

    init {
        connect()
        repository.library
            .onEach { library ->
                if (library != null && library.tracks.isNotEmpty() && (controller?.mediaItemCount ?: 0) == 0) {
                    preloadInitial()
                }
            }
            .launchIn(viewModelScope)
    }

    // ---------------------------------------------------------------- connection

    private fun connect() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val token = SessionToken(context, ComponentName(context, MusicService::class.java))
            val future = MediaController.Builder(context, token).buildAsync()
            val connected = suspendCancellableCoroutine<MediaController?> { continuation ->
                future.addListener(
                    {
                        try {
                            continuation.resume(future.get())
                        } catch (throwable: Throwable) {
                            continuation.resume(null)
                        }
                    },
                    ContextCompat.getMainExecutor(context),
                )
                continuation.invokeOnCancellation { MediaController.releaseFuture(future) }
            }
            if (connected == null) {
                _state.update { it.copy(connected = false, error = "无法连接播放服务") }
                return@launch
            }
            controller = connected
            connected.addListener(listener)
            connected.shuffleModeEnabled = prefs.shuffle()
            connected.repeatMode = RepeatMode.fromStored(prefs.repeatMode()).toPlayerMode()
            startTicker()
            refreshState()
            preloadInitial()
        }
    }

    private suspend fun preloadInitial() {
        val player = controller ?: return
        if (player.mediaItemCount > 0) return
        var library = repository.library.value
        if (library == null || library.tracks.isEmpty()) {
            library = repository.refresh().getOrNull()
        }
        val tracks = library?.tracks.orEmpty()
        if (tracks.isEmpty()) return
        val withArtwork = maxOf(tracks.size, prefs.queueIds().size) <= MAX_QUEUE_WITH_ARTWORK

        val savedQueue = prefs.queueIds().mapNotNull { library?.byId?.get(it) }
        if (savedQueue.isNotEmpty()) {
            val items = savedQueue.mapIndexed { index, track ->
                buildMediaItem(track, withArtwork || index == prefs.queueIndex())
            }
            val index = prefs.queueIndex().coerceIn(0, items.lastIndex)
            player.setMediaItems(items, index, prefs.lastPositionMs().coerceAtLeast(0L))
            player.prepare()
            player.playWhenReady = false
            refreshState()
            return
        }

        val lastId = prefs.lastTrackId()
        val track = tracks.firstOrNull { it.id == lastId } ?: tracks.first()
        player.setMediaItem(buildMediaItem(track, withArtwork = true))
        player.seekTo(prefs.lastPositionMs().coerceAtLeast(0L))
        player.prepare()
        player.playWhenReady = false
        refreshState()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            var ticks = 0
            while (true) {
                val player = controller
                if (player != null) {
                    _state.update {
                        it.copy(
                            positionMs = player.currentPosition.coerceAtLeast(0L),
                            durationMs = player.duration.takeIf { duration -> duration > 0L } ?: 0L,
                        )
                    }
                    ticks += 1
                    if (ticks % 20 == 0) persistPosition(player)
                }
                delay(250L)
            }
        }
    }

    // ------------------------------------------------------------------- actions

    fun playTracks(tracks: List<Track>, startIndex: Int) {
        val player = controller ?: return
        if (tracks.isEmpty()) return
        viewModelScope.launch {
            val withArtwork = tracks.size <= MAX_QUEUE_WITH_ARTWORK
            val items = tracks.mapIndexed { index, track ->
                buildMediaItem(track, withArtwork || index == startIndex)
            }
            player.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0L)
            player.prepare()
            player.playWhenReady = true
            refreshState()
        }
    }

    fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            player.playWhenReady = true
        }
        refreshState()
    }

    fun next() {
        controller?.seekToNextMediaItem()
        refreshState()
    }

    fun previous() {
        controller?.seekToPreviousMediaItem()
        refreshState()
    }

    fun seekTo(positionMs: Long) {
        val player = controller ?: return
        val duration = player.duration
        val target = if (duration > 0L) positionMs.coerceIn(0L, duration) else positionMs.coerceAtLeast(0L)
        player.seekTo(target)
        _state.update { it.copy(positionMs = target) }
    }

    fun playQueueAt(index: Int) {
        val player = controller ?: return
        if (index !in 0 until player.mediaItemCount) return
        player.seekTo(index, 0L)
        player.playWhenReady = true
        refreshState()
    }

    fun playNext(track: Track) {
        val player = controller ?: return
        if (player.mediaItemCount == 0) {
            playTracks(listOf(track), 0)
            return
        }
        viewModelScope.launch {
            val index = (player.currentMediaItemIndex + 1).coerceIn(0, player.mediaItemCount)
            player.addMediaItem(index, buildMediaItem(track, withArtwork = true))
            refreshState()
        }
    }

    fun enqueue(track: Track) {
        val player = controller ?: return
        viewModelScope.launch {
            player.addMediaItem(buildMediaItem(track, withArtwork = false))
            if (player.playbackState == Player.STATE_IDLE) {
                player.prepare()
                player.playWhenReady = true
            }
            refreshState()
        }
    }

    fun toggleShuffle() {
        val player = controller ?: return
        player.shuffleModeEnabled = !player.shuffleModeEnabled
        prefs.saveShuffle(player.shuffleModeEnabled)
        refreshState()
    }

    fun cycleRepeat() {
        val player = controller ?: return
        val next = _state.value.repeat.next()
        player.repeatMode = next.toPlayerMode()
        prefs.saveRepeatMode(next.ordinal)
        _state.update { it.copy(repeat = next) }
    }

    fun toggleFavorite(track: Track) {
        prefs.toggleFavorite(track.id)
    }

    fun retry() {
        val player = controller ?: return
        _state.update { it.copy(error = null) }
        player.prepare()
        player.playWhenReady = true
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimer.start(viewModelScope, minutes)
    }

    fun cancelSleepTimer() {
        sleepTimer.cancel()
    }

    fun nudgeVolume(up: Boolean) {
        _volumePercent.value = volumeController.adjust(up)
        showVolumeHud()
    }

    fun setVolumePercent(percent: Int) {
        _volumePercent.value = volumeController.setPercent(percent)
        showVolumeHud()
    }

    // -------------------------------------------------------------------- helpers

    private fun showVolumeHud() {
        _volumeHudVisible.value = true
        hudJob?.cancel()
        hudJob = viewModelScope.launch {
            delay(1_500L)
            _volumeHudVisible.value = false
        }
    }

    private fun refreshState() {
        val player = controller ?: return
        val library = repository.library.value
        val count = player.mediaItemCount
        val queue = if (library == null) {
            emptyList()
        } else {
            (0 until count).mapNotNull { index -> library.byId[player.getMediaItemAt(index).mediaId] }
        }
        persistQueue(player)
        val current = player.currentMediaItem?.mediaId?.let { library?.byId?.get(it) }
        _state.update {
            it.copy(
                connected = true,
                currentTrack = current,
                queue = queue,
                queueIndex = player.currentMediaItemIndex,
                isPlaying = player.isPlaying,
                isBuffering = player.playbackState == Player.STATE_BUFFERING,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = player.duration.takeIf { duration -> duration > 0L } ?: 0L,
                shuffle = player.shuffleModeEnabled,
                repeat = RepeatMode.fromPlayer(player.repeatMode),
            )
        }
    }

    private fun persistQueue(player: Player) {
        val count = player.mediaItemCount
        if (count == 0) return
        val ids = (0 until count).map { player.getMediaItemAt(it).mediaId }
        val signature = ids.joinToString("|")
        val index = player.currentMediaItemIndex
        if (signature == savedQueueSignature && index == savedQueueIndex) return
        savedQueueSignature = signature
        savedQueueIndex = index
        prefs.saveQueue(ids, index)
    }

    private fun persistPosition(player: Player) {
        val mediaId = player.currentMediaItem?.mediaId ?: return
        prefs.savePlaybackState(mediaId, player.currentPosition.coerceAtLeast(0L))
    }

    private suspend fun buildMediaItem(track: Track, withArtwork: Boolean): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
        if (withArtwork) {
            val bytes = runCatching { artworkLoader.loadNotificationArt(track) }.getOrNull()
            if (bytes != null) {
                metadata.setArtworkData(bytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
            }
        }
        return MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(track.uri)
            .setMediaMetadata(metadata.build())
            .build()
    }

    private fun describe(error: PlaybackException): String = when (error.errorCode) {
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "音频文件不存在"
        PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> "没有读取音频的权限"
        PlaybackException.ERROR_CODE_DECODING_FAILED,
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
        -> "音频文件无法解码"
        PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> "读取音频失败"
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        -> "读取音频失败"
        else -> error.message ?: "播放出错"
    }

    override fun onCleared() {
        tickerJob?.cancel()
        hudJob?.cancel()
        controller?.removeListener(listener)
        controller?.release()
        controller = null
        super.onCleared()
    }

    private companion object {
        const val MAX_QUEUE_WITH_ARTWORK = 60
    }
}
