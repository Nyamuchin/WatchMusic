package com.watchmusic.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.watchmusic.R
import com.watchmusic.data.model.Playlist
import com.watchmusic.data.model.Track
import com.watchmusic.ui.components.CircleIconButton
import com.watchmusic.ui.components.EmptyState
import com.watchmusic.ui.components.PlayingBars
import com.watchmusic.ui.components.TrackArtwork
import com.watchmusic.ui.components.WatchScreen
import com.watchmusic.util.TimeFormat

@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    tracks: List<Track>,
    currentTrackId: String?,
    isPlaying: Boolean,
    accent: Color,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onAddSongs: () -> Unit,
    onTrackClick: (Int) -> Unit,
    onRemoveTrack: (String) -> Unit,
) {
    var removing by remember { mutableStateOf<Track?>(null) }

    WatchScreen(
        title = playlist.name,
        subtitle = stringResource(R.string.songs_count, tracks.size),
        onBack = onBack,
        actions = {
            CircleIconButton(
                icon = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.add_songs),
                onClick = onAddSongs,
                size = 28.dp,
                iconSize = 17.dp,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
        },
    ) {
        if (tracks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = stringResource(R.string.empty_playlist_tracks),
                    subtitle = stringResource(R.string.empty_playlist_tracks_subtitle),
                    icon = Icons.Rounded.PlaylistPlay,
                    action = {
                        TextButton(onClick = onAddSongs) {
                            Text(
                                text = stringResource(R.string.add_songs),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    },
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlaylistCover(covers = tracks.take(4), size = 56.dp, cornerRadius = 16.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = stringResource(R.string.songs_count, tracks.size),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Button(
                            onClick = onPlayAll,
                            modifier = Modifier.height(34.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.play_all),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
                items(tracks, key = { it.id }) { track ->
                    PlaylistTrackRow(
                        track = track,
                        isCurrent = track.id == currentTrackId,
                        isPlaying = isPlaying,
                        accent = accent,
                        onClick = { onTrackClick(tracks.indexOf(track)) },
                        onLongClick = { removing = track },
                    )
                }
            }
        }
    }

    val track = removing
    if (track != null) {
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(track.title, style = MaterialTheme.typography.titleSmall) },
            text = { Text(stringResource(R.string.remove_from_playlist), style = MaterialTheme.typography.bodySmall) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveTrack(track.id)
                        removing = null
                    },
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { removing = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistTrackRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    accent: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrackArtwork(
            track = track,
            modifier = Modifier.size(34.dp),
            cornerRadius = 10.dp,
            sizePx = 120,
            glyphSize = 14.dp,
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isCurrent) accent else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isCurrent) {
            PlayingBars(accent = accent, playing = isPlaying)
        } else {
            Text(
                text = TimeFormat.clock(track.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
