package com.watchmusic.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/** Haptics wrapper that honours the user preference. */
class Haptics(private val feedback: HapticFeedback, private val enabled: Boolean) {
    fun tap() {
        if (enabled) feedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

@Composable
fun rememberHaptics(enabled: Boolean): Haptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback, enabled) { Haptics(feedback, enabled) }
}
