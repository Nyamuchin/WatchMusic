package com.watchmusic.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * Renders [content] with its dp sizes multiplied by [factor] while keeping the font scale
 * untouched, so a block can be shrunk to fit a space that is too small for it.
 */
@Composable
fun DensityScale(factor: Float, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val adjusted = density.density * factor
    if (adjusted == density.density) {
        content()
    } else {
        CompositionLocalProvider(
            LocalDensity provides Density(density = adjusted, fontScale = 1f),
        ) {
            content()
        }
    }
}
