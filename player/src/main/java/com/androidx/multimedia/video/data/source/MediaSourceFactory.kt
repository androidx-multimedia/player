package com.androidx.multimedia.video.data.source

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import com.androidx.multimedia.video.domain.model.VideoSource
import java.io.File

object MediaSourceFactory {

    fun createMediaItem(context: Context, source: VideoSource): MediaItem {
        val builder = MediaItem.Builder()

        source.title?.let { title ->
            builder.setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .build()
            )
        }

        return when (source) {
            is VideoSource.Network -> {
                builder.setUri(source.url)
                if (source.url.endsWith(".m3u8", ignoreCase = true)) {
                    builder.setMimeType(MimeTypes.APPLICATION_M3U8)
                }
                builder.build()
            }
            is VideoSource.LocalUri -> {
                builder.setUri(source.uri).build()
            }
            is VideoSource.FilePath -> {
                val file = File(source.path)
                val uri = Uri.fromFile(file)
                builder.setUri(uri).build()
            }
            is VideoSource.Asset -> {
                val assetUri = Uri.parse("asset:///${source.assetPath}")
                builder.setUri(assetUri).build()
            }
            is VideoSource.RawResource -> {
                val rawUri = Uri.parse("android.resource://${context.packageName}/${source.resId}")
                builder.setUri(rawUri).build()
            }
        }
    }
}
