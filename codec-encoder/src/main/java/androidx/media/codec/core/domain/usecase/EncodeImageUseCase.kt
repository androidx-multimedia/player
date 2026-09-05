package androidx.media.codec.core.domain.usecase

import androidx.media.codec.core.domain.model.EncodeResult
import androidx.media.codec.core.domain.model.MediaFile
import androidx.media.codec.core.domain.repository.MediaRepository

class EncodeImageUseCase(
    private val repository: MediaRepository
) {
    suspend fun execute(mediaFile: MediaFile): EncodeResult {
        return repository.encodeImage(mediaFile)
    }
}
