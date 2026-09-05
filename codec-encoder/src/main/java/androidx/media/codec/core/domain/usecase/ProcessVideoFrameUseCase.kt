package androidx.media.codec.core.domain.usecase

import androidx.media.codec.core.domain.model.EncodeResult
import androidx.media.codec.core.domain.model.FrameResult
import androidx.media.codec.core.domain.model.MediaFile
import androidx.media.codec.core.domain.repository.MediaRepository

sealed class VideoProcessingResult {
    data class Success(val playableUri: String) : VideoProcessingResult()
    data class Error(val message: String) : VideoProcessingResult()
}

class ProcessVideoFrameUseCase(
    private val repository: MediaRepository
) {
    suspend fun compressVideo(mediaFile: MediaFile): EncodeResult {
        return repository.encodeVideo(mediaFile)
    }

    suspend fun renderVideo(mediaFile: MediaFile, storageUrl: String): FrameResult {
        return repository.renderFrameToStorage(mediaFile, storageUrl)
    }

    suspend fun processFrame(mediaFile: MediaFile, storageUrl: String? = null): VideoProcessingResult {
        val encodeResult = repository.encodeVideo(mediaFile)
        if (!encodeResult.success) {
            return VideoProcessingResult.Error(encodeResult.errorMessage ?: "Video frame encoding failed")
        }

        val compressedFile = mediaFile.copy(
            uri = encodeResult.outputUri,
            filePath = encodeResult.outputUri
        )

        val frameResult = repository.renderFrameToStorage(compressedFile, storageUrl ?: "")
        return if (frameResult.success && frameResult.storageUrl.isNotBlank()) {
            VideoProcessingResult.Success(frameResult.storageUrl)
        } else {
            VideoProcessingResult.Success(compressedFile.uri)
        }
    }
}
