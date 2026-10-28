package com.watchmusic.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.watchmusic.ui.theme.WatchMotion

/** Previous / play-pause / next row, sized for a 204dp wide watch screen. */
@Composable
fun TransportControls(
    isPlaying: Boolean,
    isBuffering: Boolean,
    hasPrevious: Boolean,
    hasNext: Boolean,
    accent: Color,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(
            icon = Icons.Rounded.SkipPrevious,
            contentDescription = "上一首",
            onClick = onPrevious,
            size = 38.dp,
            iconSize = 20.dp,
            enabled = hasPrevious,
        )
        Spacer(Modifier.width(12.dp))
        PlayPauseButton(
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            accent = accent,
            onClick = onTogglePlay,
        )
        Spacer(Modifier.width(12.dp))
        CircleIconButton(
            icon = Icons.Rounded.SkipNext,
            contentDescription = "下一首",
            onClick = onNext,
            size = 38.dp,
            iconSize = 20.dp,
            enabled = hasNext,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    isBuffering: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) WatchMotion.PLAY_PRESS_SCALE else 1f,
        animationSpec = WatchMotion.press,
        label = "playPress",
    )
    val corner by animateDpAsState(
        targetValue = if (pressed) 18.dp else 28.dp,
        animationSpec = WatchMotion.pressSpec(),
        label = "playCorner",
    )
    val onAccent = lerp(Color.Black, accent, 0.18f)
    Box(
        modifier = Modifier
            .size(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(corner))
            .background(
                Brush.linearGradient(
                    listOf(
                        lerp(accent, Color.White, 0.18f),
                        lerp(accent, Color.Black, 0.42f),
                    ),
                ),
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isBuffering) {
            LoadingIndicator(
                modifier = Modifier.size(22.dp),
                color = onAccent,
            )
        } else {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "暂停" else "播放",
                tint = onAccent,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

/** Three animated bars used to mark the row that is currently playing. */
@Composable
fun PlayingBars(accent: Color, modifier: Modifier = Modifier, playing: Boolean = true) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        val heights = remember(playing) { if (playing) floatArrayOf(0.45f, 1f, 0.65f) else floatArrayOf(0.4f, 0.4f, 0.4f) }
        heights.forEachIndexed { index, target ->
            val animated by animateFloatAsState(
                targetValue = if (playing) target else 0.35f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(
                        durationMillis = 420 + index * 160,
                    ),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
                ),
                label = "bar$index",
            )
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .size(width = 2.5.dp, height = (12 * animated).dp)
                    .clip(CircleShape)
                    .background(accent),
            )
        }
    }
}
