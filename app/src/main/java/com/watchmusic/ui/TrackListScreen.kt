package com.watchmusic.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.watchmusic.R
import com.watchmusic.data.model.Album
import com.watchmusic.data.model.Artist
import com.watchmusic.data.model.Track
import com.watchmusic.ui.components.CircleIconButton
import com.watchmusic.ui.components.EmptyState
import com.watchmusic.ui.components.TrackRow
import com.watchmusic.ui.components.WatchScreen

/** What a track list is showing; drives title, empty state and playback intent. */
sealed interface TrackListSource {
    data object All : TrackListSource
    data object Favorites : TrackListSource
    data object Recent : TrackListSource
    data class AlbumTracks(val album: Album) : TrackListSource
    data class ArtistTracks(val artist: Artist) : TrackListSource
}

@Composable
fun TrackListScreen(
    source: TrackListSource,
    tracks: List<Track>,
    currentTrackId: String?,
    isPlaying: Boolean,
    accent: Color,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onTrackClick: (Int) -> Unit,
    onTrackLongPress: (Track) -> Unit,
) {
    val title = when (source) {
        TrackListSource.All -> stringResource(R.string.all_songs)
        TrackListSource.Favorites -> stringResource(R.string.favorites)
        TrackListSource.Recent -> stringResource(R.string.recent)
        is TrackListSource.AlbumTracks -> source.album.title
        is TrackListSource.ArtistTracks -> source.artist.name
    }
    val subtitle = when (source) {
        is TrackListSource.AlbumTracks -> source.album.artist
        is TrackListSource.ArtistTracks -> stringResource(R.string.albums_count, source.artist.albumCount)
        else -> stringResource(R.string.songs_count, tracks.size)
    }

    WatchScreen(
        title = title,
        subtitle = subtitle,
        onBack = onBack,
        actions = {
            if (tracks.isNotEmpty()) {
                CircleIconButton(
                    icon = Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(R.string.play_all),
                    onClick = onPlayAll,
                    size = 28.dp,
                    iconSize = 18.dp,
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                    contentColor = MaterialTheme.colorScheme.primary,
                )
            }
        },
    ) {
        if (tracks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val (emptyTitle, emptySubtitle, icon) = when (source) {
                    TrackListSource.Favorites -> Triple(
                        stringResource(R.string.empty_favorites),
                        stringResource(R.string.empty_favorites_subtitle),
                        Icons.Rounded.Favorite,
                    )
                    TrackListSource.Recent -> Triple(
                        stringResource(R.string.empty_recent),
                        null,
                        Icons.Rounded.LibraryMusic,
                    )
                    else -> Triple(
                        stringResource(R.string.empty_library),
                        stringResource(R.string.empty_library_subtitle),
                        Icons.Rounded.LibraryMusic,
                    )
                }
                EmptyState(title = emptyTitle, subtitle = emptySubtitle, icon = icon)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top,
            ) {
                items(tracks, key = { it.id }) { track ->
                    TrackRow(
                        track = track,
                        isCurrent = track.id == currentTrackId,
                        isPlaying = isPlaying,
                        accent = accent,
                        onClick = { onTrackClick(tracks.indexOf(track)) },
                        onLongClick = { onTrackLongPress(track) },
                    )
                }
            }
        }
    }
}

/** Shared header action showing how many items a screen holds. */
@Composable
internal fun CountLabel(count: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.songs_count, count),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(end = 8.dp).fillMaxWidth(),
    )
}
