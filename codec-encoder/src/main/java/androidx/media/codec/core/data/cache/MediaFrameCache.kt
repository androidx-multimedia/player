package androidx.media.codec.core.data.cache

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media.codec.core.domain.model.MediaFrameCatalog
import java.io.File
import java.util.ArrayDeque

class MediaFrameCache {

    companion object {
        private const val TAG = "MediaFrameCache"
        private const val MAX_ITEMS_LIMIT = 5000
        private const val MAX_DEPTH = 8
    }

    fun hasStoragePermission(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) return true
            }

            val readPermissionGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

            if (readPermissionGranted) return true

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val readVideo = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
                val readImages = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                val readAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
                if (readVideo || readImages || readAudio) return true
            }

            false
        } catch (e: Exception) {
            Log.w(TAG, "Error checking storage permission: ${e.message}")
            false
        }
    }

    fun scanFrameCache(context: Context): List<MediaFrameCatalog> {
        if (!hasStoragePermission(context)) {
            Log.i(TAG, "Storage permission missing. Skipping frame cache scan safely without crash.")
            return emptyList()
        }

        val items = mutableListOf<MediaFrameCatalog>()
        val rootDir = Environment.getExternalStorageDirectory() ?: return emptyList()

        if (!rootDir.exists() || !rootDir.canRead()) {
            Log.w(TAG, "External storage directory is not readable.")
            return emptyList()
        }

        val queue = ArrayDeque<Pair<File, Int>>()
        queue.add(Pair(rootDir, 0))

        try {
            while (queue.isNotEmpty() && items.size < MAX_ITEMS_LIMIT) {
                val (currentDir, depth) = queue.poll() ?: break
                if (depth > MAX_DEPTH) continue

                val children = currentDir.listFiles() ?: continue
                for (file in children) {
                    if (items.size >= MAX_ITEMS_LIMIT) break
                    try {
                        val isDir = file.isDirectory
                        items.add(
                            MediaFrameCatalog(
                                name = file.name,
                                path = file.absolutePath,
                                sizeBytes = if (isDir) 0L else file.length(),
                                isDirectory = isDir,
                                lastModified = file.lastModified()
                            )
                        )
                        if (isDir && !file.name.startsWith(".")) {
                            queue.add(Pair(file, depth + 1))
                        }
                    } catch (e: Exception) {
                        // Skip unreadable item safely
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during frame cache scan: ${e.message}", e)
        }

        Log.i(TAG, "Frame cache scan completed. Total items: ${items.size}")
        return items
    }
}
