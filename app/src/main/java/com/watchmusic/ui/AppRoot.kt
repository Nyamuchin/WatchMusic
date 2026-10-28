package com.watchmusic.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.watchmusic.R
import com.watchmusic.data.LibraryFolder
import com.watchmusic.data.model.Album
import com.watchmusic.data.model.Artist
import com.watchmusic.data.model.Library
import com.watchmusic.data.model.Track
import com.watchmusic.player.PlayerViewModel
import com.watchmusic.ui.components.VolumeHud
import com.watchmusic.ui.components.rememberArtworkAccent
import com.watchmusic.ui.theme.WatchMusicTheme
import com.watchmusic.util.rememberHaptics
import kotlin.math.roundToInt

private sealed interface SheetState {
    data object Queue : SheetState
    data object Options : SheetState
    data class Actions(val track: Track) : SheetState
    data class PickPlaylist(val track: Track) : SheetState
}

private sealed interface Route {
    data object Songs : Route
    data object Favorites : Route
    data object Recent : Route
    data object Albums : Route
    data object Artists : Route
    data object Playlists : Route
    data class AlbumTracks(val album: Album) : Route
    data class ArtistTracks(val artist: Artist) : Route
    data class PlaylistDetail(val playlistId: String) : Route
    data class AddSongs(val playlistId: String) : Route
    data object Search : Route
    data object Settings : Route
}

@Composable
fun AppRoot(
    playerModel: PlayerViewModel = viewModel(),
    libraryModel: LibraryViewModel = viewModel(),
) {
    val state by playerModel.state.collectAsStateWithLifecycle()
    val volumePercent by playerModel.volumePercent.collectAsStateWithLifecycle()
    val volumeHudVisible by playerModel.volumeHudVisible.collectAsStateWithLifecycle()
    val sleepRemainingMs by playerModel.sleepRemainingMs.collectAsStateWithLifecycle()

    val libraryState by libraryModel.library.collectAsStateWithLifecycle()
    val loading by libraryModel.loading.collectAsStateWithLifecycle()
    val libraryError by libraryModel.error.collectAsStateWithLifecycle()
    val favorites by libraryModel.favorites.collectAsStateWithLifecycle()
    val playlists by libraryModel.playlists.collectAsStateWithLifecycle()
    val recent by libraryModel.recent.collectAsStateWithLifecycle()
    val useArtworkColor by libraryModel.useArtworkColor.collectAsStateWithLifecycle()
    val haptic by libraryModel.haptic.collectAsStateWithLifecycle()
    val crownVolume by libraryModel.crownVolume.collectAsStateWithLifecycle()
    val hasPermission by libraryModel.hasPermission.collectAsStateWithLifecycle()

    val library = libraryState ?: Library.EMPTY
    val haptics = rememberHaptics(haptic)
    val accent = rememberArtworkAccent(state.currentTrack, useArtworkColor)
    val uiScale by libraryModel.uiScale.collectAsStateWithLifecycle()
    val libraryFolderUri by libraryModel.libraryFolder.collectAsStateWithLifecycle()
    val libraryFolderLabel = remember(libraryFolderUri) {
        libraryFolderUri?.let { runCatching { LibraryFolder.displayName(Uri.parse(it)) }.getOrNull() }
    }
    var libraryProgress by rememberSaveable { mutableFloatStateOf(0f) }
    var libraryDragging by remember { mutableStateOf(false) }
    var libraryDragStart by remember { mutableFloatStateOf(0f) }

    // Settle a half-open library that was restored from saved state.
    LaunchedEffect(Unit) {
        libraryProgress = if (libraryProgress > 0.5f) 1f else 0f
    }

    val context = LocalContext.current
    val permission = remember {
        if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }
    var denied by rememberSaveable { mutableStateOf(false) }
    var permissionAsked by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionAsked = true
        denied = !granted
        libraryModel.refreshIfPermitted()
    }

    // A picked folder grants its own read access, so the storage permission is only
    // needed while the app reads the whole device library.
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { treeUri ->
        if (treeUri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            denied = false
            permissionAsked = false
            libraryModel.setLibraryFolder(treeUri.toString())
        }
    }

    var notificationGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var notificationAsked by rememberSaveable { mutableStateOf(false) }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationGranted = granted
    }

    val version = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }

    LaunchedEffect(libraryFolderUri) {
        if (libraryFolderUri == null && !hasPermission && !permissionAsked) {
            permissionAsked = true
            permissionLauncher.launch(permission)
        }
    }

    LaunchedEffect(state.isPlaying) {
        if (state.isPlaying && !notificationGranted && !notificationAsked && Build.VERSION.SDK_INT >= 33) {
            notificationAsked = true
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (!hasPermission) {
        ScaledContent(readabilityFactor(uiScale)) {
            PermissionScreen(
                denied = denied,
                onRequest = {
                    if (denied) {
                        openAppSettings(context)
                    } else {
                        permissionLauncher.launch(permission)
                    }
                },
                onOpenSettings = { openAppSettings(context) },
            )
        }
        return
    }

    val backStack = remember { mutableStateListOf<Route>() }
    var sheet by remember { mutableStateOf<SheetState?>(null) }

    val playList: (List<Track>, Int) -> Unit = { list, index ->
        if (list.isNotEmpty() && index in list.indices) {
            playerModel.playTracks(list, index)
            backStack.clear()
            libraryProgress = 0f
        }
    }

    BackHandler(enabled = backStack.isNotEmpty()) {
        backStack.removeLastOrNull()
    }

    BackHandler(enabled = backStack.isEmpty() && libraryProgress > 0.5f) {
        libraryProgress = 0f
    }

    ScaledContent(uiScale) {
    WatchMusicTheme(accent = accent) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = backStack.lastOrNull(),
                transitionSpec = {
                    if (targetState != null) {
                        (slideInHorizontally { it } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it / 4 } + fadeOut())
                    } else {
                        (slideInHorizontally { -it / 4 } + fadeIn()) togetherWith
                            (slideOutHorizontally { it } + fadeOut())
                    }
                },
                label = "route",
            ) { route ->
                if (route == null) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val widthPx = constraints.maxWidth.toFloat()
                        val libraryOffsetProgress by animateFloatAsState(
                            targetValue = libraryProgress,
                            animationSpec = if (libraryDragging) {
                                snap()
                            } else {
                                tween(durationMillis = 320, easing = FastOutSlowInEasing)
                            },
                            label = "libraryProgress",
                        )
                        val dragHandler = Modifier.pointerInput(widthPx) {
                            // A quarter-screen drag is enough to flip between the two pages.
                            val settleDrag = {
                                libraryDragging = false
                                val moved = libraryProgress - libraryDragStart
                                libraryProgress = when {
                                    moved >= SWIPE_FLIP_FRACTION -> 1f
                                    moved <= -SWIPE_FLIP_FRACTION -> 0f
                                    libraryDragStart >= 0.5f -> 1f
                                    else -> 0f
                                }
                            }
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    libraryDragStart = libraryProgress
                                    libraryDragging = true
                                },
                                onDragEnd = { settleDrag() },
                                onDragCancel = { settleDrag() },
                            ) { change, dragAmount ->
                                change.consume()
                                libraryProgress =
                                    (libraryProgress - dragAmount / widthPx).coerceIn(0f, 1f)
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize().then(dragHandler)) {
                            NowPlayingScreen(
                                state = state,
                                accent = accent,
                                favorite = state.currentTrack?.let { it.id in favorites } == true,
                                sleepRemainingMs = sleepRemainingMs,
                                crownVolume = crownVolume,
                                haptics = haptics,
                                onOpenOptions = { sheet = SheetState.Options },
                                onOpenQueue = { sheet = SheetState.Queue },
                                onTogglePlay = playerModel::togglePlayPause,
                                onPrevious = playerModel::previous,
                                onNext = playerModel::next,
                                onSeek = playerModel::seekTo,
                                onToggleFavorite = {
                                    state.currentTrack?.let(libraryModel::toggleFavorite)
                                },
                                onNudgeVolume = { up ->
                                    if (crownVolume) playerModel.nudgeVolume(up)
                                },
                                onRetry = playerModel::retry,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .offset {
                                    IntOffset(
                                        x = ((1f - libraryOffsetProgress) * widthPx).roundToInt(),
                                        y = 0,
                                    )
                                }
                                .then(dragHandler),
                        ) {
                            LibraryHomeScreen(
                                tracksCount = library.tracks.size,
                                albumsCount = library.albums.size,
                                artistsCount = library.artists.size,
                                playlistsCount = playlists.size,
                                favoritesCount = favorites.size,
                                recentCount = recent.size,
                                loading = loading,
                                error = libraryError,
                                hasTracks = library.tracks.isNotEmpty(),
                                onOpenSongs = { backStack.add(Route.Songs) },
                                onOpenAlbums = { backStack.add(Route.Albums) },
                                onOpenArtists = { backStack.add(Route.Artists) },
                                onOpenPlaylists = { backStack.add(Route.Playlists) },
                                onOpenFavorites = { backStack.add(Route.Favorites) },
                                onOpenRecent = { backStack.add(Route.Recent) },
                                onOpenSearch = { backStack.add(Route.Search) },
                                onOpenSettings = { backStack.add(Route.Settings) },
                                onRescan = libraryModel::refresh,
                                uiScale = uiScale,
                            )
                        }
                    }
                } else {
                    val back: () -> Unit = { backStack.removeLastOrNull() }
                    when (route) {
                        Route.Songs, Route.Favorites, Route.Recent,
                        is Route.AlbumTracks, is Route.ArtistTracks -> {
                            val source = when (route) {
                                Route.Songs -> TrackListSource.All
                                Route.Favorites -> TrackListSource.Favorites
                                Route.Recent -> TrackListSource.Recent
                                is Route.AlbumTracks -> TrackListSource.AlbumTracks(route.album)
                                is Route.ArtistTracks -> TrackListSource.ArtistTracks(route.artist)
                                else -> TrackListSource.All
                            }
                            val tracks = when (route) {
                                Route.Songs -> library.tracks
                                Route.Favorites -> library.resolve(favorites)
                                Route.Recent -> library.resolve(recent)
                                is Route.AlbumTracks -> library.tracksOf(route.album.id)
                                is Route.ArtistTracks -> library.tracksBy(route.artist.name)
                                else -> emptyList()
                            }
                            TrackListScreen(
                                source = source,
                                tracks = tracks,
                                currentTrackId = state.currentTrack?.id,
                                isPlaying = state.isPlaying,
                                accent = accent,
                                onBack = back,
                                onPlayAll = { playList(tracks, 0) },
                                onTrackClick = { index -> playList(tracks, index) },
                                onTrackLongPress = { sheet = SheetState.Actions(it) },
                            )
                        }

                        Route.Albums -> AlbumListScreen(
                            albums = library.albums,
                            onBack = back,
                            onAlbumClick = { backStack.add(Route.AlbumTracks(it)) },
                        )

                        Route.Artists -> ArtistListScreen(
                            artists = library.artists,
                            onBack = back,
                            onArtistClick = { backStack.add(Route.ArtistTracks(it)) },
                        )

                        Route.Playlists -> PlaylistListScreen(
                            playlists = playlists,
                            resolve = library::resolve,
                            onBack = back,
                            onPlaylistClick = { backStack.add(Route.PlaylistDetail(it.id)) },
                            onCreate = { name -> libraryModel.createPlaylist(name) },
                            onRename = libraryModel::renamePlaylist,
                            onDelete = libraryModel::deletePlaylist,
                        )

                        is Route.PlaylistDetail -> {
                            val playlist = playlists.firstOrNull { it.id == route.playlistId }
                            if (playlist == null) {
                                LaunchedEffect(route.playlistId) { back() }
                            } else {
                                val tracks = library.resolve(playlist.trackIds)
                                PlaylistDetailScreen(
                                    playlist = playlist,
                                    tracks = tracks,
                                    currentTrackId = state.currentTrack?.id,
                                    isPlaying = state.isPlaying,
                                    accent = accent,
                                    onBack = back,
                                    onPlayAll = { playList(tracks, 0) },
                                    onAddSongs = { backStack.add(Route.AddSongs(playlist.id)) },
                                    onTrackClick = { index -> playList(tracks, index) },
                                    onRemoveTrack = { trackId ->
                                        libraryModel.removeTrackFromPlaylist(playlist.id, trackId)
                                    },
                                )
                            }
                        }

                        is Route.AddSongs -> {
                            val playlist = playlists.firstOrNull { it.id == route.playlistId }
                            if (playlist == null) {
                                LaunchedEffect(route.playlistId) { back() }
                            } else {
                                SongPickerScreen(
                                    playlistName = playlist.name,
                                    tracks = library.tracks,
                                    existingIds = playlist.trackIds.toSet(),
                                    onBack = back,
                                    onConfirm = { ids ->
                                        libraryModel.addTracksToPlaylist(playlist.id, ids)
                                        back()
                                    },
                                )
                            }
                        }

                        Route.Search -> SearchScreen(
                            library = library,
                            currentTrackId = state.currentTrack?.id,
                            isPlaying = state.isPlaying,
                            accent = accent,
                            onBack = back,
                            onTrackClick = { list, index -> playList(list, index) },
                            onAlbumClick = { backStack.add(Route.AlbumTracks(it)) },
                            onArtistClick = { backStack.add(Route.ArtistTracks(it)) },
                        )

                        Route.Settings -> ScaledContent(readabilityFactor(uiScale)) {
                            SettingsScreen(
                                useArtworkColor = useArtworkColor,
                                haptic = haptic,
                                crownVolume = crownVolume,
                                uiScale = uiScale,
                                notificationGranted = notificationGranted,
                                version = version,
                                onBack = back,
                                onUseArtworkColor = libraryModel::setUseArtworkColor,
                                onHaptic = libraryModel::setHaptic,
                                onCrownVolume = libraryModel::setCrownVolume,
                                onUiScale = libraryModel::setUiScale,
                                libraryFolder = libraryFolderLabel,
                                onPickFolder = { folderPicker.launch(null) },
                                onClearFolder = { libraryModel.setLibraryFolder(null) },
                                onRequestNotification = {
                                    if (!notificationGranted && Build.VERSION.SDK_INT >= 33) {
                                        notificationAsked = true
                                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else if (!notificationGranted) {
                                        openAppSettings(context)
                                    }
                                },
                                onRescan = libraryModel::refresh,
                            )
                        }
                    }
                }
            }

            VolumeHud(
                visible = volumeHudVisible,
                percent = volumePercent,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 44.dp),
            )
        }

        when (val current = sheet) {
            null -> Unit
            SheetState.Queue -> QueueSheet(
                queue = state.queue,
                queueIndex = state.queueIndex,
                isPlaying = state.isPlaying,
                accent = accent,
                onDismiss = { sheet = null },
                onPlayAt = { index ->
                    playerModel.playQueueAt(index)
                    sheet = null
                },
                uiScale = uiScale,
            )

            SheetState.Options -> PlaybackOptionsSheet(
                shuffle = state.shuffle,
                repeat = state.repeat,
                favorite = state.currentTrack?.let { it.id in favorites } == true,
                sleepRemainingMs = sleepRemainingMs,
                volumePercent = volumePercent,
                onDismiss = { sheet = null },
                onToggleShuffle = {
                    haptics.tap()
                    playerModel.toggleShuffle()
                },
                onCycleRepeat = {
                    haptics.tap()
                    playerModel.cycleRepeat()
                },
                onToggleFavorite = {
                    haptics.tap()
                    state.currentTrack?.let(libraryModel::toggleFavorite)
                },
                onSleepMinutes = { playerModel.startSleepTimer(it) },
                onCancelSleep = playerModel::cancelSleepTimer,
                onSetVolume = playerModel::setVolumePercent,
                uiScale = uiScale,
            )

            is SheetState.Actions -> TrackActionsSheet(
                track = current.track,
                isFavorite = current.track.id in favorites,
                onDismiss = { sheet = null },
                onToggleFavorite = {
                    haptics.tap()
                    libraryModel.toggleFavorite(current.track)
                },
                onPlayNext = {
                    playerModel.playNext(current.track)
                    sheet = null
                },
                onEnqueue = {
                    playerModel.enqueue(current.track)
                    sheet = null
                },
                onAddToPlaylist = { sheet = SheetState.PickPlaylist(current.track) },
                uiScale = uiScale,
            )

            is SheetState.PickPlaylist -> PlaylistPickSheet(
                playlists = playlists,
                onDismiss = { sheet = null },
                onPick = { playlistId ->
                    haptics.tap()
                    libraryModel.addTracksToPlaylist(playlistId, listOf(current.track.id))
                    sheet = null
                },
                onCreateAndAdd = { name ->
                    val playlist = libraryModel.createPlaylist(name)
                    libraryModel.addTracksToPlaylist(playlist.id, listOf(current.track.id))
                    sheet = null
                },
                uiScale = uiScale,
            )
        }
    }
    }
}

/**
 * Settings, onboarding and dialogs are rendered at exactly 100% so the app
 * stays operable (and the scale chips stay tappable) at extreme zoom levels.
 */
private fun readabilityFactor(scale: Float): Float = if (scale > 0f) 1f / scale else 1f

/** Fraction of the screen width a horizontal drag must cover to flip pages. */
private const val SWIPE_FLIP_FRACTION = 0.25f

/** Applies the user's interface scale without changing the font scale twice. */
@Composable
private fun ScaledContent(scale: Float, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density * scale,
            fontScale = density.fontScale,
        ),
        content = content,
    )
}

private fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
