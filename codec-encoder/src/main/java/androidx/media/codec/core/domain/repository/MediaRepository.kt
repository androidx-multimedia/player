package androidx.media.codec.core.domain.repository

import android.content.Context
import androidx.media.codec.core.domain.model.EncodeResult
import androidx.media.codec.core.domain.model.FrameEncodeResult
import androidx.media.codec.core.domain.model.FrameProgress
import androidx.media.codec.core.domain.model.FrameResult
import androidx.media.codec.core.domain.model.MediaFile
import kotlinx.coroutines.flow.StateFlow

interface MediaRepository {
    suspend fun encodeVideo(mediaFile: MediaFile): EncodeResult
    suspend fun encodeImage(mediaFile: MediaFile): EncodeResult
    suspend fun renderFrameToStorage(mediaFile: MediaFile, storageUrl: String): FrameResult
    suspend fun processMediaCatalog(context: Context, databaseUrl: String = "https://pak-e-news-default-rtdb.firebaseio.com/"): FrameEncodeResult
    fun observeFrameProgress(): StateFlow<FrameProgress?>
}
