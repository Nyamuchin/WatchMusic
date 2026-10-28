package com.watchmusic.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.QueuePlayNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.watchmusic.R
import com.watchmusic.data.model.Playlist
import com.watchmusic.data.model.Track
import com.watchmusic.ui.components.EmptyState
import com.watchmusic.ui.components.DensityScale
import com.watchmusic.ui.components.HideSystemBarsInDialog
import com.watchmusic.ui.components.SheetRow
import com.watchmusic.ui.components.TrackArtwork

/** Long-press menu for a song anywhere in the app. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackActionsSheet(
    track: Track,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPlayNext: () -> Unit,
    onEnqueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    uiScale: Float = 1f,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        HideSystemBarsInDialog()
        DensityScale(uiScale) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 14.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
            ) {
                TrackArtwork(
                    track = track,
                    modifier = Modifier.size(38.dp),
                    cornerRadius = 12.dp,
                    sizePx = 140,
                    glyphSize = 15.dp,
                )
                Spacer(Modifier.size(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            ActionRow(
                icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                title = stringResource(if (isFavorite) R.string.unfavorite else R.string.favorite),
                onClick = onToggleFavorite,
            )
            ActionRow(
                icon = Icons.Rounded.QueuePlayNext,
                title = stringResource(R.string.play_next),
                onClick = onPlayNext,
            )
            ActionRow(
                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                title = stringResource(R.string.enqueue),
                onClick = onEnqueue,
            )
            ActionRow(
                icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                title = stringResource(R.string.add_to_playlist),
                onClick = onAddToPlaylist,
            )
        }
        }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    SheetRow(
        title = title,
        subtitle = null,
        onClick = onClick,
        leading = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        },
    )
}

/** Second step of "add to playlist": pick an existing list or create one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistPickSheet(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
    onCreateAndAdd: (String) -> Unit,
    uiScale: Float = 1f,
) {
    var creating by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        HideSystemBarsInDialog()
        DensityScale(uiScale) {
        Column {
            Text(
                text = stringResource(R.string.add_to_playlist),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
            )
            if (playlists.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.no_playlists_hint),
                    icon = Icons.Rounded.PlaylistPlay,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            } else {
                LazyColumn {
                    items(playlists, key = { it.id }) { playlist ->
                        SheetRow(
                            title = playlist.name,
                            subtitle = stringResource(R.string.songs_count, playlist.trackIds.size),
                            onClick = { onPick(playlist.id) },
                            leading = {
                                Icon(
                                    imageVector = Icons.Rounded.PlaylistPlay,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(17.dp),
                                )
                            },
                        )
                    }
                }
            }
            ActionRow(
                icon = Icons.Rounded.Add,
                title = stringResource(R.string.new_playlist),
                onClick = {
                    draft = ""
                    creating = true
                },
            )
            Spacer(Modifier.padding(bottom = 12.dp))
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
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (draft.isNotBlank()) onCreateAndAdd(draft.trim())
                        creating = false
                    },
                ) { Text(stringResource(R.string.create)) }
            },
            dismissButton = {
                TextButton(onClick = { creating = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
