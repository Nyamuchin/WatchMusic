package com.watchmusic.player

import androidx.media3.common.Player
import com.watchmusic.data.model.Track

enum class RepeatMode {
    OFF,
    ALL,
    ONE;

    fun toPlayerMode(): Int = when (this) {
        OFF -> Player.REPEAT_MODE_OFF
        ALL -> Player.REPEAT_MODE_ALL
        ONE -> Player.REPEAT_MODE_ONE
    }

    fun next(): RepeatMode = when (this) {
        OFF -> ALL
        ALL -> ONE
        ONE -> OFF
    }

    companion object {
        fun fromPlayer(mode: Int): RepeatMode = when (mode) {
            Player.REPEAT_MODE_ALL -> ALL
            Player.REPEAT_MODE_ONE -> ONE
            else -> OFF
        }

        fun fromStored(value: Int): RepeatMode = when (value) {
            ALL.ordinal -> ALL
            ONE.ordinal -> ONE
            else -> OFF
        }
    }
}

/** Single immutable snapshot of everything the player UI needs. */
data class PlaybackUiState(
    val connected: Boolean = false,
    val currentTrack: Track? = null,
    val queue: List<Track> = emptyList(),
    val queueIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.OFF,
    val error: String? = null,
) {
    val hasNext: Boolean
        get() = queue.isNotEmpty() && (queueIndex < queue.lastIndex || repeat == RepeatMode.ALL)

    val hasPrevious: Boolean
        get() = queue.isNotEmpty() && (queueIndex > 0 || repeat == RepeatMode.ALL)
}
