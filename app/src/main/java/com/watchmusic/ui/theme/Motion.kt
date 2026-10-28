package com.watchmusic.ui.theme

import androidx.compose.animation.core.FloatSpringSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/** Motion tokens shared by the expressive components of the app. */
object WatchMotion {
    /** Snappy press feedback used on every tappable control. */
    val press = FloatSpringSpec(
        dampingRatio = 0.55f,
        stiffness = 520f,
    )

    /** Softer spring for panel / sheet entrance. */
    val panel = FloatSpringSpec(
        dampingRatio = 0.78f,
        stiffness = 320f,
    )

    /** Very soft spring for artwork cross fades. */
    val artwork = FloatSpringSpec(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow,
    )

    /** Same curve as [panel], usable for Dp/Color animations. */
    fun <T> panelSpec(): SpringSpec<T> = spring(dampingRatio = 0.78f, stiffness = 320f)

    /** Same curve as [press], usable for Dp/Color animations. */
    fun <T> pressSpec(): SpringSpec<T> = spring(dampingRatio = 0.55f, stiffness = 520f)

    const val PRESS_SCALE = 0.92f
    const val PLAY_PRESS_SCALE = 0.9f
}
