package com.androidx.multimedia.video.domain.repository

import com.androidx.multimedia.video.domain.model.AspectRatio
import com.androidx.multimedia.video.domain.model.PlaybackPosition
import com.androidx.multimedia.video.domain.model.PlayerState
import com.androidx.multimedia.video.domain.model.TrackSelectionState
import com.androidx.multimedia.video.domain.model.VideoSource
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface defining video player business operations and state streams.
 */
interface VideoPlayerRepository {
    val playerState: StateFlow<PlayerState>
    val playbackPosition: StateFlow<PlaybackPosition>
    val aspectRatio: StateFlow<AspectRatio>
    val playbackSpeed: StateFlow<Float>
    val trackSelectionState: StateFlow<TrackSelectionState>
    val isControlsLocked: StateFlow<Boolean>
    val volume: StateFlow<Float>

    fun initialize(source: VideoSource, autoPlay: Boolean = true)
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun seekByOffset(offsetMs: Long)
    fun setPlaybackSpeed(speed: Float)
    fun setAspectRatio(aspectRatio: AspectRatio)
    fun setVolume(volume: Float)
    fun selectAudioTrack(trackId: String?)
    fun selectSubtitleTrack(trackId: String?)
    fun toggleControlsLock(): Boolean
    fun release()
}
