package com.watchmusic.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.watchmusic.WatchMusicApp
import com.watchmusic.data.AccentColor
import com.watchmusic.data.model.Track
import com.watchmusic.ui.theme.WatchPrimary

/** Loads (and caches) the cover art of a track as a Compose bitmap. */
@Composable
fun rememberTrackBitmap(track: Track?, sizePx: Int = 240): ImageBitmap? {
    val context = LocalContext.current
    val loader = remember(context) { (context.applicationContext as WatchMusicApp).container.artwork }
    val bitmap by produceState<ImageBitmap?>(initialValue = null, track?.id, sizePx) {
        value = track?.let { loader.load(it, sizePx) }?.asImageBitmap()
    }
    return bitmap
}

/** Accent color derived from the artwork, or the default lavender. */
@Composable
fun rememberArtworkAccent(track: Track?, enabled: Boolean): Color {
    if (!enabled || track == null) return WatchPrimary
    val bitmap = rememberTrackBitmap(track, 96)
    return remember(bitmap) {
        val argb = bitmap?.asAndroidBitmap()?.let { AccentColor.fromBitmap(it) }
        if (argb != null) Color(argb) else WatchPrimary
    }
}

/**
 * Cover art with a deterministic gradient placeholder, so the UI never shows an
 * empty hole when a track has no embedded picture.
 */
@Composable
fun TrackArtwork(
    track: Track?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    sizePx: Int = 240,
    glyphSize: Dp = 16.dp,
    contentDescription: String? = null,
) {
    val bitmap = rememberTrackBitmap(track, sizePx)
    val seeded = remember(track?.id) { AccentColor.seededColors(track?.seed ?: 0) }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Brush.linearGradient(listOf(Color(seeded.first), Color(seeded.second)))),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = contentDescription,
                tint = Color.White.copy(alpha = 0.72f),
                modifier = Modifier.size(glyphSize),
            )
        }
    }
}
