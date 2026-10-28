package com.watchmusic.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Simple countdown timer that pauses playback when it reaches zero. */
class SleepTimer(private val onExpire: () -> Unit) {

    private val _remainingMs = MutableStateFlow(0L)
    val remainingMs: StateFlow<Long> = _remainingMs.asStateFlow()

    private var job: Job? = null

    fun start(scope: CoroutineScope, minutes: Int) {
        cancel()
        if (minutes <= 0) return
        _remainingMs.value = minutes * 60_000L
        job = scope.launch {
            while (_remainingMs.value > 0L) {
                delay(1_000L)
                val next = (_remainingMs.value - 1_000L).coerceAtLeast(0L)
                _remainingMs.value = next
            }
            onExpire()
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _remainingMs.value = 0L
    }
}
