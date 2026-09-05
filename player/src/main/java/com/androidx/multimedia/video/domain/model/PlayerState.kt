package com.androidx.multimedia.video.domain.model

sealed class PlayerState {
    object Idle : PlayerState()
    object Buffering : PlayerState()
    object Playing : PlayerState()
    object Paused : PlayerState()
    object Ended : PlayerState()
    data class Error(val message: String, val cause: Throwable? = null) : PlayerState()
}

data class PlaybackPosition(
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L
) {
    val progressFraction: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}
