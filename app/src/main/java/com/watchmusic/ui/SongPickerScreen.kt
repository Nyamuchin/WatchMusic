package com.watchmusic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.watchmusic.R
import com.watchmusic.data.model.Track
import com.watchmusic.ui.components.EmptyState
import com.watchmusic.ui.components.TrackArtwork
import com.watchmusic.ui.components.WatchScreen

/** Multi-select list used when adding songs to a playlist. */
@Composable
fun SongPickerScreen(
    playlistName: String,
    tracks: List<Track>,
    existingIds: Set<String>,
    onBack: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    val selected = remember { mutableStateListOf<String>() }

    WatchScreen(
        title = stringResource(R.string.add_songs),
        subtitle = playlistName,
        onBack = onBack,
        actions = {
            TextButton(
                onClick = { onConfirm(selected.toList()) },
                enabled = selected.isNotEmpty(),
            ) {
                Text(
                    text = if (selected.isEmpty()) {
                        stringResource(R.string.done)
                    } else {
                        "${stringResource(R.string.done)} (${selected.size})"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected.isEmpty()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
        },
    ) {
        if (tracks.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                EmptyState(
                    title = stringResource(R.string.empty_library),
                    subtitle = stringResource(R.string.empty_library_subtitle),
                    icon = Icons.Rounded.LibraryMusic,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(tracks, key = { it.id }) { track ->
                    val isExisting = track.id in existingIds
                    val checked = track.id in selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable(enabled = !isExisting) {
                                if (checked) selected.remove(track.id) else selected.add(track.id)
                            }
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TrackArtwork(
                            track = track,
                            modifier = Modifier.size(32.dp),
                            cornerRadius = 10.dp,
                            sizePx = 120,
                            glyphSize = 13.dp,
                        )
                        Spacer(Modifier.width(8.dp))
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
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        if (isExisting) {
                            Text(
                                text = stringResource(R.string.done),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Checkbox(checked = checked, onCheckedChange = null)
                        }
                    }
                }
            }
        }
    }
}
