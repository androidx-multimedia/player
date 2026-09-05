package com.androidx.multimedia.video.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.androidx.multimedia.video.domain.model.VideoSource
import com.androidx.multimedia.video.ui.view.VideoPlayerView

@Composable
fun VideoPlayer(
    videoSource: VideoSource,
    modifier: Modifier = Modifier,
    state: VideoPlayerState = rememberVideoPlayerState(),
    onBackClick: (() -> Unit)? = null,
    onFullscreenToggle: ((Boolean) -> Unit)? = null
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            VideoPlayerView(context).apply {
                state.playerView = this
                onBackClick?.let { listener ->
                    setOnBackClickListener { listener() }
                }
                onFullscreenToggle?.let { listener ->
                    setOnFullscreenClickListener(listener)
                }
                load(videoSource)
            }
        },
        update = { view ->
            state.playerView = view
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            state.release()
        }
    }
}
