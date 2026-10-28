package com.watchmusic.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.watchmusic.R
import com.watchmusic.data.AccentColor
import com.watchmusic.data.model.Playlist
import com.watchmusic.data.model.Track
import com.watchmusic.ui.components.CircleIconButton
import com.watchmusic.ui.components.EmptyState
import com.watchmusic.ui.components.WatchScreen
import com.watchmusic.ui.components.rememberTrackBitmap

@Composable
fun PlaylistListScreen(
    playlists: List<Playlist>,
    resolve: (List<String>) -> List<Track>,
    onBack: () -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onCreate: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Playlist?>(null) }
    var draft by remember { mutableStateOf("") }

    WatchScreen(
        title = stringResource(R.string.playlists),
        subtitle = stringResource(R.string.playlists_count, playlists.size),
        onBack = onBack,
        actions = {
            CircleIconButton(
                icon = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.new_playlist),
                onClick = {
                    draft = ""
                    creating = true
                },
                size = 28.dp,
                iconSize = 17.dp,
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                contentColor = MaterialTheme.colorScheme.primary,
            )
        },
    ) {
        if (playlists.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = stringResource(R.string.empty_playlists),
                    subtitle = stringResource(R.string.empty_playlists_subtitle),
                    icon = Icons.Rounded.PlaylistPlay,
                    action = {
                        TextButton(onClick = { creating = true }) {
                            Text(
                                text = stringResource(R.string.new_playlist),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    },
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(playlists, key = { it.id }) { playlist ->
                    PlaylistRow(
                        playlist = playlist,
                        covers = resolve(playlist.trackIds).take(4),
                        onClick = { onPlaylistClick(playlist) },
                        onLongClick = {
                            draft = playlist.name
                            editing = playlist
                        },
                    )
                }
            }
        }
    }

    if (creating) {
        AlertDialog(
            onDismissRequest = { creating = false },
            title = { Text(stringResource(R.string.new_playlist), style = MaterialTheme.typography.titleSmall) },
            text = {
                TextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.playlist_name_hint)) },
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (draft.isNotBlank()) onCreate(draft.trim())
                        creating = false
                    },
                ) { Text(stringResource(R.string.create)) }
            },
            dismissButton = {
                TextButton(onClick = { creating = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    val target = editing
    if (target != null) {
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(target.name, style = MaterialTheme.typography.titleSmall) },
            text = {
                TextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (draft.isNotBlank()) onRename(target.id, draft.trim())
                        editing = null
                    },
                ) { Text(stringResource(R.string.rename)) }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            onDelete(target.id)
                            editing = null
                        },
                    ) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { editing = null }) { Text(stringResource(R.string.cancel)) }
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistRow(
    playlist: Playlist,
    covers: List<Track>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaylistCover(covers = covers, size = 40.dp)
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.songs_count, playlist.trackIds.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(14.dp),
        )
    }
}

/** 2x2 mosaic for playlists, falling back to a tinted placeholder. */
@Composable
fun PlaylistCover(
    covers: List<Track>,
    size: Dp,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
) {
    if (covers.isEmpty()) {
        val seeded = remember { AccentColor.seededColors(7) }
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(cornerRadius))
                .background(Brush.linearGradient(listOf(Color(seeded.first), Color(seeded.second)))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.PlaylistPlay,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(size * 0.42f),
            )
        }
        return
    }
    if (covers.size < 4) {
        Box(modifier = modifier.size(size).clip(RoundedCornerShape(cornerRadius))) {
            PlaylistCell(track = covers.first(), modifier = Modifier.fillMaxSize())
        }
        return
    }
    Column(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius)),
    ) {
        Row(modifier = Modifier.weight(1f)) {
            PlaylistCell(track = covers[0], modifier = Modifier.weight(1f).fillMaxHeight())
            PlaylistCell(track = covers[1], modifier = Modifier.weight(1f).fillMaxHeight())
        }
        Row(modifier = Modifier.weight(1f)) {
            PlaylistCell(track = covers[2], modifier = Modifier.weight(1f).fillMaxHeight())
            PlaylistCell(track = covers[3], modifier = Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun PlaylistCell(track: Track, modifier: Modifier = Modifier) {
    val bitmap = rememberTrackBitmap(track, 120)
    val seeded = remember(track.id) { AccentColor.seededColors(track.seed) }
    Box(
        modifier = modifier.background(Brush.linearGradient(listOf(Color(seeded.first), Color(seeded.second)))),
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
