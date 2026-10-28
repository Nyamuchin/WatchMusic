package com.watchmusic.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance

/** Picks a readable foreground for an arbitrary artwork-derived accent color. */
private fun onAccent(accent: Color): Color =
    if (accent.luminance() > 0.5f) Color(0xFF1B1030) else Color(0xFFFFFBFE)

/**
 * Material 3 Expressive theme for WatchMusic.
 *
 * The scheme stays dark on purpose: the now playing screen is a full bleed
 * artwork, and only the accent colors follow the current cover.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WatchMusicTheme(
    accent: Color = WatchPrimary,
    content: @Composable () -> Unit,
) {
    val scheme = darkColorScheme(
        primary = accent,
        onPrimary = onAccent(accent),
        primaryContainer = accent.copy(alpha = 0.26f).compositeOver(WatchSurfaceContainer),
        onPrimaryContainer = WatchOnSurface,
        inversePrimary = WatchSurfaceContainerHighest,
        secondary = AccentMint,
        onSecondary = Color(0xFF06281F),
        secondaryContainer = Color(0xFF12352A),
        onSecondaryContainer = Color(0xFFB7F5DC),
        tertiary = AccentCoral,
        onTertiary = Color(0xFF3A100B),
        tertiaryContainer = Color(0xFF4A1712),
        onTertiaryContainer = Color(0xFFFFDAD4),
        background = WatchSurface,
        onBackground = WatchOnSurface,
        surface = WatchSurface,
        onSurface = WatchOnSurface,
        surfaceVariant = WatchSurfaceContainerHigh,
        onSurfaceVariant = WatchOnSurfaceVariant,
        surfaceTint = accent,
        surfaceContainerLowest = Color(0xFF07060A),
        surfaceContainerLow = WatchSurfaceContainer,
        surfaceContainer = WatchSurfaceContainer,
        surfaceContainerHigh = WatchSurfaceContainerHigh,
        surfaceContainerHighest = WatchSurfaceContainerHighest,
        outline = WatchOutline,
        outlineVariant = WatchOutline.copy(alpha = 0.55f),
        error = WatchError,
        onError = Color(0xFF3B0907),
        scrim = Color.Black,
    )
    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.expressive(),
        shapes = WatchShapes,
        typography = WatchTypography,
        content = content,
    )
}
