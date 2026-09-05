package com.androidx.multimedia.video.domain.usecase

import com.androidx.multimedia.video.domain.model.AspectRatio
import com.androidx.multimedia.video.domain.model.PlaybackPosition
import com.androidx.multimedia.video.domain.model.PlayerState
import com.androidx.multimedia.video.domain.model.TrackSelectionState
import com.androidx.multimedia.video.domain.model.VideoSource
import com.androidx.multimedia.video.domain.repository.VideoPlayerRepository
import kotlinx.coroutines.flow.StateFlow

class InitializePlayerUseCase(private val repository: VideoPlayerRepository) {
    operator fun invoke(source: VideoSource, autoPlay: Boolean = true) {
        repository.initialize(source, autoPlay)
    }
}

class PlayVideoUseCase(private val repository: VideoPlayerRepository) {
    operator fun invoke() {
        repository.play()
    }
}

class PauseVideoUseCase(private val repository: VideoPlayerRepository) {
    operator fun invoke() {
        repository.pause()
    }
}

class SeekToUseCase(private val repository: VideoPlayerRepository) {
    operator fun invoke(positionMs: Long) {
        repository.seekTo(positionMs)
    }
}

class SeekByOffsetUseCase(private val repository: VideoPlayerRepository) {
    operator fun invoke(offsetMs: Long) {
        repository.seekByOffset(offsetMs)
    }
}

class SetPlaybackSpeedUseCase(private val repository: VideoPlayerRepository) {
    operator fun invoke(speed: Float) {
        repository.setPlaybackSpeed(speed)
    }
}

class SetAspectRatioUseCase(private val repository: VideoPlayerRepository) {
    operator fun invoke(aspectRatio: AspectRatio) {
        repository.setAspectRatio(aspectRatio)
    }
}

class SelectAudioTrackUseCase(private val repository: VideoPlayerRepository) {
    operator fun invoke(trackId: String?) {
        repository.selectAudioTrack(trackId)
    }
}

class SelectSubtitleTrackUseCase(private val repository: VideoPlayerRepository) {
    operator fun invoke(trackId: String?) {
        repository.selectSubtitleTrack(trackId)
    }
}

class SetVolumeUseCase(private val repository: VideoPlayerRepository) {
    operator fun invoke(volume: Float) {
        repository.setVolume(volume)
    }
}

class ToggleControlsLockUseCase(private val repository: VideoPlayerRepository) {
    operator fun invoke(): Boolean {
        return repository.toggleControlsLock()
    }
}

class GetPlayerStateUseCase(private val repository: VideoPlayerRepository) {
    val playerState: StateFlow<PlayerState> get() = repository.playerState
    val playbackPosition: StateFlow<PlaybackPosition> get() = repository.playbackPosition
    val aspectRatio: StateFlow<AspectRatio> get() = repository.aspectRatio
    val playbackSpeed: StateFlow<Float> get() = repository.playbackSpeed
    val trackSelectionState: StateFlow<TrackSelectionState> get() = repository.trackSelectionState
    val isControlsLocked: StateFlow<Boolean> get() = repository.isControlsLocked
    val volume: StateFlow<Float> get() = repository.volume
}
