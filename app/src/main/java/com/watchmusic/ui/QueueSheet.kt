package com.watchmusic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.watchmusic.R
import com.watchmusic.data.model.Track
import com.watchmusic.ui.components.DensityScale
import com.watchmusic.ui.components.EmptyState
import com.watchmusic.ui.components.HideSystemBarsInDialog
import com.watchmusic.ui.components.PlayingBars
import com.watchmusic.ui.components.TrackArtwork
import com.watchmusic.util.TimeFormat

/** Real playback order, so shuffle is visible to the user. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    queue: List<Track>,
    queueIndex: Int,
    isPlaying: Boolean,
    accent: Color,
    onDismiss: () -> Unit,
    onPlayAt: (Int) -> Unit,
    uiScale: Float = 1f,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        HideSystemBarsInDialog()
        DensityScale(uiScale) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.queue),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.weight(1f))
                if (queue.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.songs_count, queue.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (queue.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.empty_queue),
                    subtitle = stringResource(R.string.empty_queue_subtitle),
                    modifier = Modifier.padding(bottom = 24.dp),
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    itemsIndexed(queue, key = { _, track -> track.id }) { index, track ->
                        val current = index == queueIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clickable { onPlayAt(index) }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TrackArtwork(
                                track = track,
                                modifier = Modifier.size(30.dp),
                                cornerRadius = 9.dp,
                                sizePx = 120,
                                glyphSize = 12.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (current) accent else MaterialTheme.colorScheme.onSurface,
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
                            Spacer(Modifier.width(6.dp))
                            if (current) {
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
                }
            }
        }
        }
    }
}
