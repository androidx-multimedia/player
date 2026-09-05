package androidx.multimedia.video.ui.dialog

import android.content.Context
import androidx.appcompat.app.AlertDialog

object PlaybackSpeedDialog {

    private val speeds = floatArrayOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    private val speedLabels = arrayOf("0.25x", "0.5x", "0.75x", "1.0x (Normal)", "1.25x", "1.5x", "2.0x")

    fun showSpeedDialog(
        context: Context,
        currentSpeed: Float,
        onSpeedSelected: (Float) -> Unit
    ) {
        var selectedIndex = 3 // default 1.0x
        speeds.forEachIndexed { index, speed ->
            if (kotlin.math.abs(speed - currentSpeed) < 0.05f) {
                selectedIndex = index
            }
        }

        AlertDialog.Builder(context)
            .setTitle("Playback Speed")
            .setSingleChoiceItems(speedLabels, selectedIndex) { dialog, which ->
                onSpeedSelected(speeds[which])
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
