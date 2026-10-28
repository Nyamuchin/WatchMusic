package com.watchmusic.player

import android.content.Context
import android.media.AudioManager

/** Thin wrapper around the music stream so both the crown and the slider use the same path. */
class VolumeController(context: Context) {

    private val audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun percent(): Int {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 100 / max).coerceIn(0, 100)
    }

    fun adjust(up: Boolean): Int {
        val direction = if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
        return percent()
    }

    fun setPercent(percent: Int): Int {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val target = (percent.coerceIn(0, 100) * max / 100).coerceIn(0, max)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        return percent()
    }
}
