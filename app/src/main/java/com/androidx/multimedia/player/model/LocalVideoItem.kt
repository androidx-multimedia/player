package com.androidx.multimedia.player.model

import android.net.Uri

data class LocalVideoItem(
    val id: Long,
    val title: String,
    val path: String?,
    val uri: Uri,
    val durationMs: Long,
    val sizeBytes: Long
) {
    val formattedDuration: String
        get() {
            val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
            val seconds = totalSeconds % 60
            val minutes = (totalSeconds / 60) % 60
            val hours = totalSeconds / 3600
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%d:%02d", minutes, seconds)
            }
        }

    val formattedSize: String
        get() {
            val mb = sizeBytes.toDouble() / (1024 * 1024)
            return if (mb >= 1024) {
                String.format("%.2f GB", mb / 1024)
            } else {
                String.format("%.1f MB", mb)
            }
        }
}
