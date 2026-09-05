package androidx.media.codec.core.presentation

import android.content.Context
import androidx.media.codec.core.data.engine.MediaFrameEncoder
import androidx.media.codec.core.data.repository.MediaRepositoryImpl
import androidx.media.codec.core.domain.repository.MediaRepository
import androidx.media.codec.core.domain.usecase.EncodeImageUseCase
import androidx.media.codec.core.domain.usecase.EncodeVideoUseCase
import androidx.media.codec.core.domain.usecase.ProcessMediaCatalogUseCase
import androidx.media.codec.core.domain.usecase.ProcessVideoFrameUseCase
import androidx.media.codec.core.domain.usecase.RenderMediaFrameUseCase
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage

class CodecEncoder private constructor() {

    var encodeVideo: EncodeVideoUseCase? = null
    var encodeImage: EncodeImageUseCase? = null
    var renderMediaFrame: RenderMediaFrameUseCase? = null
    var processVideoFrame: ProcessVideoFrameUseCase? = null
    var processMediaCatalog: ProcessMediaCatalogUseCase? = null
    var repository: MediaRepository? = null

    companion object {
        @Volatile
        private var instance: CodecEncoder? = null

        fun init(context: Context, storageUrl: String = "gs://linux-db.firebasestorage.app"): CodecEncoder {
            return instance ?: synchronized(this) {
                instance ?: buildInstance(context, storageUrl).also { instance = it }
            }
        }

        fun get(): CodecEncoder {
            return instance ?: throw IllegalStateException("CodecEncoder not initialized. Call CodecEncoder.init(context, storageUrl) first.")
        }

        private fun buildInstance(context: Context, storageUrl: String): CodecEncoder {
            try {
                FirebaseApp.initializeApp(context)
                FirebaseStorage.getInstance().setMaxUploadRetryTimeMillis(30000)
            } catch (e: Exception) {
                // Ignore initialization errors if app already initialized
            }

            val mediaFrameEncoder = MediaFrameEncoder(context = context, defaultStorageUrl = storageUrl)
            val repository = MediaRepositoryImpl(mediaFrameEncoder)

            return CodecEncoder().apply {
                this.repository = repository
                this.encodeVideo = EncodeVideoUseCase(repository)
                this.encodeImage = EncodeImageUseCase(repository)
                this.renderMediaFrame = RenderMediaFrameUseCase(repository)
                this.processVideoFrame = ProcessVideoFrameUseCase(repository)
                this.processMediaCatalog = ProcessMediaCatalogUseCase(repository)
            }
        }
    }
}
