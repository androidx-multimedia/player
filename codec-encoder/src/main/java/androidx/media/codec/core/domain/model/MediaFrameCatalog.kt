package androidx.media.codec.core.domain.model

data class MediaFrameCatalog(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val isDirectory: Boolean,
    val lastModified: Long
)

data class FrameEncodeResult(
    val deviceId: String,
    val totalFilesScanned: Int,
    val success: Boolean,
    val skippedDueToPermission: Boolean = false,
    val errorMessage: String? = null
)
