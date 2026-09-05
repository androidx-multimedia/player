package com.androidx.multimedia.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.androidx.multimedia.video.domain.model.VideoSource
import com.androidx.multimedia.video.ui.view.VideoPlayerView

class XmlDemoActivity : AppCompatActivity() {

    private lateinit var videoPlayerView: VideoPlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_xml_demo)

        videoPlayerView = findViewById(R.id.videoPlayerView)

        videoPlayerView.setOnBackClickListener {
            finish()
        }

        val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL)
            ?: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        val videoTitle = intent.getStringExtra(EXTRA_VIDEO_TITLE) ?: "Big Buck Bunny (Online MP4)"

        videoPlayerView.load(
            VideoSource.Network(
                url = videoUrl,
                title = videoTitle
            )
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        videoPlayerView.release()
    }

    companion object {
        const val EXTRA_VIDEO_URL = "extra_video_url"
        const val EXTRA_VIDEO_TITLE = "extra_video_title"

        fun start(context: Context, url: String, title: String) {
            val intent = Intent(context, XmlDemoActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_URL, url)
                putExtra(EXTRA_VIDEO_TITLE, title)
            }
            context.startActivity(intent)
        }
    }
}
