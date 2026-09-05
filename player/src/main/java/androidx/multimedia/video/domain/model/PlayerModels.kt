package androidx.multimedia.video.domain.model

enum class AspectRatio(val displayName: String) {
    FIT("Fit"),
    FILL("Fill"),
    ZOOM("Zoom"),
    RATIO_16_9("16:9"),
    RATIO_4_3("4:3")
}

data class AudioTrackInfo(
    val id: String,
    val name: String,
    val language: String?,
    val isSelected: Boolean
)

data class SubtitleTrackInfo(
    val id: String,
    val name: String,
    val language: String?,
    val isSelected: Boolean
)

data class TrackSelectionState(
    val audioTracks: List<AudioTrackInfo> = emptyList(),
    val subtitleTracks: List<SubtitleTrackInfo> = emptyList()
)

sealed class GestureState {
    object None : GestureState()
    data class Brightness(val percent: Int) : GestureState()
    data class Volume(val percent: Int) : GestureState()
    data class SeekDelta(val seekDeltaMs: Long, val targetPositionMs: Long) : GestureState()
}
