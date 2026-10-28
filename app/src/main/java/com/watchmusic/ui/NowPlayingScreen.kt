package com.watchmusic.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.watchmusic.R
import com.watchmusic.data.AccentColor
import com.watchmusic.player.PlaybackUiState
import com.watchmusic.ui.components.CircleIconButton
import com.watchmusic.ui.components.DensityScale
import com.watchmusic.ui.components.TransportControls
import com.watchmusic.ui.components.WatchSeekBar
import com.watchmusic.ui.components.rememberTrackBitmap
import com.watchmusic.ui.theme.WatchError
import com.watchmusic.util.Haptics
import com.watchmusic.util.TimeFormat

/** Full-bleed cover page: the first thing you see when the app opens. */
@Composable
fun NowPlayingScreen(
    state: PlaybackUiState,
    accent: Color,
    favorite: Boolean,
    sleepRemainingMs: Long,
    crownVolume: Boolean,
    haptics: Haptics,
    onOpenOptions: () -> Unit,
    onOpenQueue: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleFavorite: () -> Unit,
    onNudgeVolume: (Boolean) -> Unit,
    onRetry: () -> Unit,
) {
    val track = state.currentTrack
    val bitmap = rememberTrackBitmap(track, 768)
    val seeded = remember(track?.id) { AccentColor.seededColors(track?.seed ?: 0) }
    val focusRequester = remember { FocusRequester() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onRotaryScrollEvent { event ->
                if (!crownVolume) {
                    false
                } else {
                    val delta = event.verticalScrollPixels
                    if (delta != 0f) onNudgeVolume(delta < 0f)
                    true
                }
            },
    ) {
        // When the UI is zoomed in the screen no longer has room for the full control stack,
        // so the stack is shrunk until it fits instead of being clipped off-screen.
        val stackFit = ((maxHeight - 46.dp) / 146.dp).coerceIn(0.55f, 1f)
        // Full-bleed artwork.
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(Color(seeded.first), Color(seeded.second)))),
                contentAlignment = Alignment.Center,
            ) {
                if (track == null) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.size(64.dp),
                    )
                }
            }
        }

        // Legibility scrims.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.55f),
                        0.30f to Color.Transparent,
                        0.52f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.92f),
                    ),
                ),
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Top actions, centred above the artwork.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(
                    icon = Icons.Rounded.MoreVert,
                    contentDescription = stringResource(R.string.more_options),
                    onClick = {
                        haptics.tap()
                        onOpenOptions()
                    },
                    size = 30.dp,
                    iconSize = 18.dp,
                )
                CircleIconButton(
                    icon = Icons.Rounded.QueueMusic,
                    contentDescription = stringResource(R.string.queue),
                    onClick = {
                        haptics.tap()
                        onOpenQueue()
                    },
                    size = 30.dp,
                    iconSize = 16.dp,
                )
            }

            Spacer(Modifier.weight(1f))

            DensityScale(stackFit) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .padding(bottom = 6.dp),
            ) {
                if (sleepRemainingMs > 0L) {
                    Row(
                        modifier = Modifier.padding(bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(11.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = TimeFormat.countdown(sleepRemainingMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track?.title ?: stringResource(R.string.nothing_playing),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = track?.artist ?: stringResource(R.string.nothing_playing_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    CircleIconButton(
                        icon = if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = stringResource(
                            if (favorite) R.string.unfavorite else R.string.favorite,
                        ),
                        onClick = {
                            haptics.tap()
                            onToggleFavorite()
                        },
                        size = 30.dp,
                        iconSize = 17.dp,
                        containerColor = Color.Transparent,
                        contentColor = if (favorite) Color(0xFFFF6B81) else Color.White,
                        enabled = track != null,
                    )
                }

                WatchSeekBar(
                    positionMs = state.positionMs,
                    durationMs = state.durationMs,
                    isPlaying = state.isPlaying,
                    accent = accent,
                    onSeek = onSeek,
                )

                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
                    Text(
                        text = TimeFormat.clock(state.positionMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.72f),
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = TimeFormat.clock(state.durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.72f),
                    )
                }

                val error = state.error
                if (error != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.labelSmall,
                            color = WatchError,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onRetry) {
                            Text(
                                text = stringResource(R.string.retry),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                            )
                        }
                    }
                } else if (!state.connected) {
                    Text(
                        text = stringResource(R.string.connecting),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }

                Spacer(Modifier.height(2.dp))
                TransportControls(
                    isPlaying = state.isPlaying,
                    isBuffering = state.isBuffering,
                    hasPrevious = state.hasPrevious,
                    hasNext = state.hasNext,
                    accent = accent,
                    onPrevious = {
                        haptics.tap()
                        onPrevious()
                    },
                    onTogglePlay = {
                        haptics.tap()
                        onTogglePlay()
                    },
                    onNext = {
                        haptics.tap()
                        onNext()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            }
        }
    }
}
