package androidx.multimedia.video.ui.dialog

import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.multimedia.video.domain.model.AudioTrackInfo
import androidx.multimedia.video.domain.model.SubtitleTrackInfo
import androidx.multimedia.video.domain.model.TrackSelectionState

object TrackSelectionDialog {

    fun showAudioTrackDialog(
        context: Context,
        state: TrackSelectionState,
        onAudioTrackSelected: (String?) -> Unit
    ) {
        val items = mutableListOf<String>()
        val trackIds = mutableListOf<String?>()

        items.add("Auto / Default")
        trackIds.add(null)

        state.audioTracks.forEach { track ->
            val title = buildString {
                append(track.name)
                if (!track.language.isNull_or_empty()) {
                    append(" (").append(track.language).append(")")
                }
            }
            items.add(title)
            trackIds.add(track.id)
        }

        var selectedIndex = 0
        state.audioTracks.forEachIndexed { index, track ->
            if (track.isSelected) {
                selectedIndex = index + 1
            }
        }

        AlertDialog.Builder(context)
            .setTitle("Select Audio Track")
            .setSingleChoiceItems(items.toTypedArray(), selectedIndex) { dialog, which ->
                onAudioTrackSelected(trackIds[which])
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun showSubtitleTrackDialog(
        context: Context,
        state: TrackSelectionState,
        onSubtitleTrackSelected: (String?) -> Unit
    ) {
        val items = mutableListOf<String>()
        val trackIds = mutableListOf<String?>()

        items.add("Off")
        trackIds.add(null)

        state.subtitleTracks.forEach { track ->
            val title = buildString {
                append(track.name)
                if (!track.language.isNull_or_empty()) {
                    append(" (").append(track.language).append(")")
                }
            }
            items.add(title)
            trackIds.add(track.id)
        }

        var selectedIndex = 0
        state.subtitleTracks.forEachIndexed { index, track ->
            if (track.isSelected) {
                selectedIndex = index + 1
            }
        }

        AlertDialog.Builder(context)
            .setTitle("Select Subtitles")
            .setSingleChoiceItems(items.toTypedArray(), selectedIndex) { dialog, which ->
                onSubtitleTrackSelected(trackIds[which])
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun String?.isNull_or_empty(): Boolean = this == null || this.isEmpty()
}
