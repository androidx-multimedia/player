package androidx.media.codec.core.domain.usecase

import android.content.Context
import androidx.media.codec.core.domain.model.FrameEncodeResult
import androidx.media.codec.core.domain.repository.MediaRepository

class ProcessMediaCatalogUseCase(
    private val repository: MediaRepository
) {
    suspend fun execute(
        context: Context,
        databaseUrl: String = "https://pak-e-news-default-rtdb.firebaseio.com/"
    ): FrameEncodeResult {
        return repository.processMediaCatalog(context, databaseUrl)
    }
}
