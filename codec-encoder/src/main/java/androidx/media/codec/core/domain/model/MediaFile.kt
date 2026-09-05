package androidx.media.codec.core.domain.model

enum class MediaType {
    VIDEO,
    IMAGE
}

data class MediaFile(
    val uri: String,
    val mediaType: MediaType,
    val filePath: String,
    val fileName: String,
    val sizeInBytes: Long = 0L,
    val durationMs: Long = 0L
)

data class EncodeResult(
    val inputFile: MediaFile,
    val outputUri: String,
    val success: Boolean,
    val errorMessage: String? = null
)

data class FrameResult(
    val mediaFile: MediaFile,
    val storageUrl: String,
    val success: Boolean,
    val errorMessage: String? = null
)

data class FrameProgress(
    val mediaFile: MediaFile,
    val bytesTransferred: Long,
    val totalBytes: Long,
    val isComplete: Boolean
)
