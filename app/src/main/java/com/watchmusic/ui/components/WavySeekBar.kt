package com.watchmusic.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.dp

/**
 * Seek bar built on the Expressive wavy progress indicator. Tapping or dragging
 * anywhere on the row seeks; the value is committed on release.
 */
@Composable
fun WatchSeekBar(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    accent: Color,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fraction = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val shown = dragFraction ?: fraction

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(durationMs) {
                if (durationMs <= 0L) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var current = (down.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    dragFraction = current
                    down.consume()
                    var pressed = true
                    while (pressed) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null) {
                            pressed = false
                        } else {
                            if (change.positionChanged()) {
                                current = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                dragFraction = current
                                change.consume()
                            }
                            pressed = change.pressed
                        }
                    }
                    dragFraction = null
                    onSeek((current * durationMs).toLong())
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        LinearWavyProgressIndicator(
            progress = { shown },
            modifier = Modifier.fillMaxWidth(),
            color = accent,
            trackColor = Color.White.copy(alpha = 0.26f),
            wavelength = 26.dp,
            waveSpeed = if (isPlaying) 18.dp else 0.dp,
        )
    }
}
