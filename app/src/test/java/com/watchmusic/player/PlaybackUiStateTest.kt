package com.watchmusic.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackUiStateTest {

    @Test
    fun repeatMode_cyclesOffAllOne() {
        assertEquals(RepeatMode.ALL, RepeatMode.OFF.next())
        assertEquals(RepeatMode.ONE, RepeatMode.ALL.next())
        assertEquals(RepeatMode.OFF, RepeatMode.ONE.next())
        assertEquals(RepeatMode.OFF, RepeatMode.OFF.next().next().next())
    }

    @Test
    fun repeatMode_decodesStoredValues() {
        assertEquals(RepeatMode.OFF, RepeatMode.fromStored(0))
        assertEquals(RepeatMode.ALL, RepeatMode.fromStored(1))
        assertEquals(RepeatMode.ONE, RepeatMode.fromStored(2))
        assertEquals(RepeatMode.OFF, RepeatMode.fromStored(99))
        assertEquals(RepeatMode.OFF, RepeatMode.fromStored(-1))
    }

    @Test
    fun defaults_describeDisconnectedPlayer() {
        val state = PlaybackUiState()
        assertFalse(state.connected)
        assertEquals(null, state.currentTrack)
        assertTrue(state.queue.isEmpty())
        assertFalse(state.isPlaying)
        assertFalse(state.shuffle)
        assertEquals(RepeatMode.OFF, state.repeat)
        assertEquals(null, state.error)
    }

    @Test
    fun navigationFlags_areFalseWithoutQueue() {
        assertFalse(PlaybackUiState().hasNext)
        assertFalse(PlaybackUiState().hasPrevious)
        assertFalse(PlaybackUiState(repeat = RepeatMode.ALL).hasNext)
        assertFalse(PlaybackUiState(repeat = RepeatMode.ALL).hasPrevious)
    }
}
