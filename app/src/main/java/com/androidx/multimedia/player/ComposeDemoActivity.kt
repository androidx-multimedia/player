package com.androidx.multimedia.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.multimedia.video.domain.model.VideoSource
import androidx.multimedia.video.ui.compose.VideoPlayer
import androidx.multimedia.video.ui.compose.rememberVideoPlayerState

class ComposeDemoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL)
            ?: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
        val videoTitle = intent.getStringExtra(EXTRA_VIDEO_TITLE) ?: "Elephant's Dream (Compose Stream)"

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val playerState = rememberVideoPlayerState()

                    VideoPlayer(
                        videoSource = VideoSource.Network(url = videoUrl, title = videoTitle),
                        modifier = Modifier.fillMaxSize(),
                        state = playerState,
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_VIDEO_URL = "extra_video_url"
        const val EXTRA_VIDEO_TITLE = "extra_video_title"

        fun start(context: Context, url: String, title: String) {
            val intent = Intent(context, ComposeDemoActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_URL, url)
                putExtra(EXTRA_VIDEO_TITLE, title)
            }
            context.startActivity(intent)
        }
    }
}
