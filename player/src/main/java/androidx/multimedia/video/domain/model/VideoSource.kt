package androidx.multimedia.video.domain.model

import android.net.Uri

/**
 * Represents a video source that can be loaded into the player.
 * Supports online network streams, local file paths, content URIs, raw resources, and assets.
 */
sealed class VideoSource {
    abstract val title: String?

    data class Network(
        val url: String,
        override val title: String? = null,
        val headers: Map<String, String> = emptyMap()
    ) : VideoSource()

    data class LocalUri(
        val uri: Uri,
        override val title: String? = null
    ) : VideoSource()

    data class FilePath(
        val path: String,
        override val title: String? = null
    ) : VideoSource()

    data class Asset(
        val assetPath: String,
        override val title: String? = null
    ) : VideoSource()

    data class RawResource(
        val resId: Int,
        override val title: String? = null
    ) : VideoSource()
}
