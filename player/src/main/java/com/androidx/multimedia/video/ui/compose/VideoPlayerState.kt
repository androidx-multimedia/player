package com.androidx.multimedia.video.ui.compose

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.androidx.multimedia.video.domain.model.AspectRatio
import com.androidx.multimedia.video.domain.model.VideoSource
import com.androidx.multimedia.video.ui.view.VideoPlayerView
import java.io.File

class VideoPlayerState {
    internal var playerView: VideoPlayerView? = null

    fun load(source: VideoSource) {
        playerView?.load(source)
    }

    fun loadUrl(url: String, title: String? = null) {
        playerView?.loadUrl(url, title)
    }

    fun loadFile(file: File, title: String? = null) {
        playerView?.loadFile(file, title)
    }

    fun loadUri(uri: Uri, title: String? = null) {
        playerView?.loadUri(uri, title)
    }

    fun play() {
        playerView?.play()
    }

    fun pause() {
        playerView?.pause()
    }

    fun seekTo(positionMs: Long) {
        playerView?.seekTo(positionMs)
    }

    fun setPlaybackSpeed(speed: Float) {
        playerView?.setPlaybackSpeed(speed)
    }

    fun setAspectRatio(aspectRatio: AspectRatio) {
        playerView?.setAspectRatio(aspectRatio)
    }

    fun release() {
        playerView?.release()
    }
}

@Composable
fun rememberVideoPlayerState(): VideoPlayerState {
    return remember { VideoPlayerState() }
}
