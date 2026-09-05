package androidx.multimedia.video.data.repository

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.multimedia.video.data.source.MediaSourceFactory
import androidx.multimedia.video.domain.model.AspectRatio
import androidx.multimedia.video.domain.model.AudioTrackInfo
import androidx.multimedia.video.domain.model.PlaybackPosition
import androidx.multimedia.video.domain.model.PlayerState
import androidx.multimedia.video.domain.model.SubtitleTrackInfo
import androidx.multimedia.video.domain.model.TrackSelectionState
import androidx.multimedia.video.domain.repository.VideoPlayerRepository
import androidx.multimedia.video.domain.model.VideoSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@OptIn(UnstableApi::class)
class ExoVideoPlayerRepository(
    private val context: Context,
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()
) : VideoPlayerRepository {

    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Idle)
    override val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _playbackPosition = MutableStateFlow(PlaybackPosition())
    override val playbackPosition: StateFlow<PlaybackPosition> = _playbackPosition.asStateFlow()

    private val _aspectRatio = MutableStateFlow(AspectRatio.FIT)
    override val aspectRatio: StateFlow<AspectRatio> = _aspectRatio.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    override val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _trackSelectionState = MutableStateFlow(TrackSelectionState())
    override val trackSelectionState: StateFlow<TrackSelectionState> = _trackSelectionState.asStateFlow()

    private val _isControlsLocked = MutableStateFlow(false)
    override val isControlsLocked: StateFlow<Boolean> = _isControlsLocked.asStateFlow()

    private val _volume = MutableStateFlow(1.0f)
    override val volume: StateFlow<Float> = _volume.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private val updatePositionRunnable = object : Runnable {
        override fun run() {
            if (exoPlayer.isPlaying) {
                updatePositionState()
            }
            handler.postDelayed(this, 500)
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_IDLE -> _playerState.value = PlayerState.Idle
                Player.STATE_BUFFERING -> _playerState.value = PlayerState.Buffering
                Player.STATE_READY -> {
                    _playerState.value = if (exoPlayer.isPlaying) PlayerState.Playing else PlayerState.Paused
                    updatePositionState()
                }
                Player.STATE_ENDED -> _playerState.value = PlayerState.Ended
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                _playerState.value = PlayerState.Playing
                handler.post(updatePositionRunnable)
            } else {
                if (exoPlayer.playbackState == Player.STATE_READY) {
                    _playerState.value = PlayerState.Paused
                }
                handler.removeCallbacks(updatePositionRunnable)
            }
            updatePositionState()
        }

        override fun onPlayerError(error: PlaybackException) {
            _playerState.value = PlayerState.Error(
                message = error.localizedMessage ?: "Playback Error",
                cause = error
            )
        }

        override fun onTracksChanged(tracks: Tracks) {
            updateTrackSelectionState(tracks)
        }
    }

    init {
        exoPlayer.addListener(playerListener)
    }

    override fun initialize(source: VideoSource, autoPlay: Boolean) {
        val mediaItem = MediaSourceFactory.createMediaItem(context, source)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = autoPlay
    }

    override fun play() {
        exoPlayer.play()
    }

    override fun pause() {
        exoPlayer.pause()
    }

    override fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs.coerceIn(0L, exoPlayer.duration.coerceAtLeast(0L)))
        updatePositionState()
    }

    override fun seekByOffset(offsetMs: Long) {
        val target = exoPlayer.currentPosition + offsetMs
        seekTo(target)
    }

    override fun setPlaybackSpeed(speed: Float) {
        val validSpeed = speed.coerceIn(0.25f, 2.0f)
        exoPlayer.setPlaybackSpeed(validSpeed)
        _playbackSpeed.value = validSpeed
    }

    override fun setAspectRatio(aspectRatio: AspectRatio) {
        _aspectRatio.value = aspectRatio
    }

    override fun setVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        exoPlayer.volume = clampedVolume
        _volume.value = clampedVolume
    }

    override fun selectAudioTrack(trackId: String?) {
        val parametersBuilder = exoPlayer.trackSelectionParameters.buildUpon()
        if (trackId == null) {
            parametersBuilder.clearOverridesOfType(C.TRACK_TYPE_AUDIO)
        } else {
            val tracks = exoPlayer.currentTracks
            for (group in tracks.groups) {
                if (group.type == C.TRACK_TYPE_AUDIO) {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        if (format.id == trackId || "${group.mediaTrackGroup.id}_$i" == trackId) {
                            parametersBuilder.setOverrideForType(
                                TrackSelectionOverride(group.mediaTrackGroup, i)
                            )
                            break
                        }
                    }
                }
            }
        }
        exoPlayer.trackSelectionParameters = parametersBuilder.build()
    }

    override fun selectSubtitleTrack(trackId: String?) {
        val parametersBuilder = exoPlayer.trackSelectionParameters.buildUpon()
        if (trackId == null) {
            parametersBuilder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        } else {
            parametersBuilder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            val tracks = exoPlayer.currentTracks
            for (group in tracks.groups) {
                if (group.type == C.TRACK_TYPE_TEXT) {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        if (format.id == trackId || "${group.mediaTrackGroup.id}_$i" == trackId) {
                            parametersBuilder.setOverrideForType(
                                TrackSelectionOverride(group.mediaTrackGroup, i)
                            )
                            break
                        }
                    }
                }
            }
        }
        exoPlayer.trackSelectionParameters = parametersBuilder.build()
    }

    override fun toggleControlsLock(): Boolean {
        val nextState = !_isControlsLocked.value
        _isControlsLocked.value = nextState
        return nextState
    }

    override fun release() {
        handler.removeCallbacks(updatePositionRunnable)
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }

    private fun updatePositionState() {
        val current = exoPlayer.currentPosition.coerceAtLeast(0L)
        val duration = exoPlayer.duration.coerceAtLeast(0L)
        val buffered = exoPlayer.bufferedPosition.coerceAtLeast(0L)
        _playbackPosition.value = PlaybackPosition(
            currentPositionMs = current,
            durationMs = duration,
            bufferedPositionMs = buffered
        )
    }

    private fun updateTrackSelectionState(tracks: Tracks) {
        val audioTracks = mutableListOf<AudioTrackInfo>()
        val subtitleTracks = mutableListOf<SubtitleTrackInfo>()

        for (group in tracks.groups) {
            val trackType = group.type
            val mediaGroup = group.mediaTrackGroup

            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                val trackId = format.id ?: "${mediaGroup.id}_$i"
                val label = format.label ?: format.language ?: "Track ${i + 1}"
                val isSelected = group.isTrackSelected(i)

                if (trackType == C.TRACK_TYPE_AUDIO) {
                    audioTracks.add(
                        AudioTrackInfo(
                            id = trackId,
                            name = label,
                            language = format.language,
                            isSelected = isSelected
                        )
                    )
                } else if (trackType == C.TRACK_TYPE_TEXT) {
                    subtitleTracks.add(
                        SubtitleTrackInfo(
                            id = trackId,
                            name = label,
                            language = format.language,
                            isSelected = isSelected
                        )
                    )
                }
            }
        }

        _trackSelectionState.value = TrackSelectionState(
            audioTracks = audioTracks,
            subtitleTracks = subtitleTracks
        )
    }
}
