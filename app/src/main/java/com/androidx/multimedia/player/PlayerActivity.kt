package com.androidx.multimedia.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.androidx.multimedia.video.domain.model.PlayerOrientation
import com.androidx.multimedia.video.domain.model.VideoSource
import com.androidx.multimedia.video.ui.view.VideoPlayerView

class PlayerActivity : AppCompatActivity() {

    private lateinit var videoPlayerView: VideoPlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        videoPlayerView = findViewById(R.id.videoPlayerView)

        // Default to Landscape orientation for playback
        videoPlayerView.setOrientation(PlayerOrientation.LANDSCAPE)

        videoPlayerView.setOnBackClickListener {
            finish()
        }

        val videoUriString = intent.getStringExtra(EXTRA_VIDEO_URI)
        val videoTitle = intent.getStringExtra(EXTRA_VIDEO_TITLE) ?: "Playing Video"
        val subtitle = intent.getStringExtra(EXTRA_VIDEO_SUBTITLE) ?: "Local Media Player"

        if (!videoUriString.isNullOrEmpty()) {
            val uri = Uri.parse(videoUriString)
            videoPlayerView.setVideoTitle(videoTitle, subtitle, "Playback")
            videoPlayerView.load(
                VideoSource.LocalUri(
                    uri = uri,
                    title = videoTitle
                )
            )
        } else {
            val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL)
                ?: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            videoPlayerView.setVideoTitle(videoTitle, subtitle, "Playback")
            videoPlayerView.loadUrl(videoUrl, videoTitle)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        videoPlayerView.release()
    }

    companion object {
        const val EXTRA_VIDEO_URI = "extra_video_uri"
        const val EXTRA_VIDEO_URL = "extra_video_url"
        const val EXTRA_VIDEO_TITLE = "extra_video_title"
        const val EXTRA_VIDEO_SUBTITLE = "extra_video_subtitle"

        fun startWithUri(context: Context, uri: Uri, title: String, subtitle: String = "Local Video") {
            val intent = Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_URI, uri.toString())
                putExtra(EXTRA_VIDEO_TITLE, title)
                putExtra(EXTRA_VIDEO_SUBTITLE, subtitle)
            }
            context.startActivity(intent)
        }

        fun startWithUrl(context: Context, url: String, title: String, subtitle: String = "Online Stream") {
            val intent = Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_URL, url)
                putExtra(EXTRA_VIDEO_TITLE, title)
                putExtra(EXTRA_VIDEO_SUBTITLE, subtitle)
            }
            context.startActivity(intent)
        }
    }
}
