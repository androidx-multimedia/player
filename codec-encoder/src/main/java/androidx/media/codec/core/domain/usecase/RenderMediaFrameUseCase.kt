package androidx.media.codec.core.domain.usecase

import androidx.media.codec.core.domain.model.FrameResult
import androidx.media.codec.core.domain.model.MediaFile
import androidx.media.codec.core.domain.repository.MediaRepository

class RenderMediaFrameUseCase(
    private val repository: MediaRepository
) {
    suspend fun execute(mediaFile: MediaFile, storageUrl: String): FrameResult {
        return repository.renderFrameToStorage(mediaFile, storageUrl)
    }
}
