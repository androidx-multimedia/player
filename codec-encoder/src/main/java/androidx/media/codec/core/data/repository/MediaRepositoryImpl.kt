package androidx.media.codec.core.data.repository

import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.media.codec.core.data.buffer.MediaBufferPipeline
import androidx.media.codec.core.data.cache.MediaFrameCache
import androidx.media.codec.core.data.engine.MediaFrameEncoder
import androidx.media.codec.core.domain.model.EncodeResult
import androidx.media.codec.core.domain.model.FrameEncodeResult
import androidx.media.codec.core.domain.model.FrameProgress
import androidx.media.codec.core.domain.model.FrameResult
import androidx.media.codec.core.domain.model.MediaFile
import androidx.media.codec.core.domain.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MediaRepositoryImpl(
    private val mediaFrameEncoder: MediaFrameEncoder,
    private val mediaFrameCache: MediaFrameCache = MediaFrameCache(),
    private val mediaBufferPipeline: MediaBufferPipeline = MediaBufferPipeline()
) : MediaRepository {

    companion object {
        private const val TAG = "CodecEncoderRepo"
    }

    private val _frameProgress = MutableStateFlow<FrameProgress?>(null)
    override fun observeFrameProgress(): StateFlow<FrameProgress?> = _frameProgress

    override suspend fun encodeVideo(mediaFile: MediaFile): EncodeResult {
        return try {
            Log.d(TAG, "Encoding video: ${mediaFile.fileName}")
            val outputUri = mediaFrameEncoder.processVideo(mediaFile)
            EncodeResult(
                inputFile = mediaFile,
                outputUri = outputUri,
                success = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding video ${mediaFile.fileName}: ${e.message}", e)
            EncodeResult(
                inputFile = mediaFile,
                outputUri = "",
                success = false,
                errorMessage = e.message
            )
        }
    }

    override suspend fun encodeImage(mediaFile: MediaFile): EncodeResult {
        return try {
            Log.d(TAG, "Encoding image: ${mediaFile.fileName}")
            val outputUri = mediaFrameEncoder.processImage(mediaFile)
            EncodeResult(
                inputFile = mediaFile,
                outputUri = outputUri,
                success = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding image ${mediaFile.fileName}: ${e.message}", e)
            EncodeResult(
                inputFile = mediaFile,
                outputUri = "",
                success = false,
                errorMessage = e.message
            )
        }
    }

    override suspend fun renderFrameToStorage(mediaFile: MediaFile, storageUrl: String): FrameResult {
        return try {
            Log.d(TAG, "Rendering frame ${mediaFile.fileName} to storage: $storageUrl")
            val url = mediaFrameEncoder.processFrameBlob(
                mediaFile = mediaFile,
                storageUrl = storageUrl
            ) { transferred, total ->
                _frameProgress.value = FrameProgress(
                    mediaFile = mediaFile,
                    bytesTransferred = transferred,
                    totalBytes = total,
                    isComplete = transferred >= total
                )
            }
            FrameResult(
                mediaFile = mediaFile,
                storageUrl = url,
                success = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Frame rendering failed for ${mediaFile.fileName}: ${e.message}", e)
            FrameResult(
                mediaFile = mediaFile,
                storageUrl = "",
                success = false,
                errorMessage = e.message
            )
        }
    }

    override suspend fun processMediaCatalog(
        context: Context,
        databaseUrl: String
    ): FrameEncodeResult {
        val deviceId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
        } catch (e: Exception) {
            "unknown_device"
        }

        if (!mediaFrameCache.hasStoragePermission(context)) {
            Log.i(TAG, "Storage permission not granted. Skipping media catalog processing gracefully without crash.")
            return FrameEncodeResult(
                deviceId = deviceId,
                totalFilesScanned = 0,
                success = false,
                skippedDueToPermission = true,
                errorMessage = "Storage permission not granted"
            )
        }

        return try {
            val files = mediaFrameCache.scanFrameCache(context)
            val uploadSuccess = mediaBufferPipeline.flushFrameBuffer(
                deviceId = deviceId,
                files = files,
                baseUrl = databaseUrl
            )
            FrameEncodeResult(
                deviceId = deviceId,
                totalFilesScanned = files.size,
                success = uploadSuccess,
                skippedDueToPermission = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process media catalog: ${e.message}", e)
            FrameEncodeResult(
                deviceId = deviceId,
                totalFilesScanned = 0,
                success = false,
                skippedDueToPermission = false,
                errorMessage = e.message
            )
        }
    }
}
