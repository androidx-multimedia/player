package androidx.multimedia.video.ui.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.multimedia.video.R
import kotlin.jvm.JvmOverloads
import androidx.multimedia.video.data.repository.ExoVideoPlayerRepository
import androidx.multimedia.video.domain.model.AspectRatio
import androidx.multimedia.video.domain.model.PlayerOrientation
import androidx.multimedia.video.domain.model.PlayerState
import androidx.multimedia.video.domain.model.VideoSource
import androidx.multimedia.video.domain.usecase.GetPlayerStateUseCase
import androidx.multimedia.video.domain.usecase.InitializePlayerUseCase
import androidx.multimedia.video.domain.usecase.PauseVideoUseCase
import androidx.multimedia.video.domain.usecase.PlayVideoUseCase
import androidx.multimedia.video.domain.usecase.SeekByOffsetUseCase
import androidx.multimedia.video.domain.usecase.SeekToUseCase
import androidx.multimedia.video.domain.usecase.SelectAudioTrackUseCase
import androidx.multimedia.video.domain.usecase.SelectSubtitleTrackUseCase
import androidx.multimedia.video.domain.usecase.SetAspectRatioUseCase
import androidx.multimedia.video.domain.usecase.SetPlaybackSpeedUseCase
import androidx.multimedia.video.domain.usecase.ToggleControlsLockUseCase
import androidx.multimedia.video.ui.dialog.PlaybackSpeedDialog
import androidx.multimedia.video.ui.dialog.TrackSelectionDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.File
import java.util.Locale

@OptIn(UnstableApi::class)
class VideoPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // Repository & UseCases (Clean Architecture)
    private val repository = ExoVideoPlayerRepository(context)
    private val initializePlayerUseCase = InitializePlayerUseCase(repository)
    private val playVideoUseCase = PlayVideoUseCase(repository)
    private val pauseVideoUseCase = PauseVideoUseCase(repository)
    private val seekToUseCase = SeekToUseCase(repository)
    private val seekByOffsetUseCase = SeekByOffsetUseCase(repository)
    private val setPlaybackSpeedUseCase = SetPlaybackSpeedUseCase(repository)
    private val setAspectRatioUseCase = SetAspectRatioUseCase(repository)
    private val selectAudioTrackUseCase = SelectAudioTrackUseCase(repository)
    private val selectSubtitleTrackUseCase = SelectSubtitleTrackUseCase(repository)
    private val toggleControlsLockUseCase = ToggleControlsLockUseCase(repository)
    private val getPlayerStateUseCase = GetPlayerStateUseCase(repository)

    // UI Elements
    private val exoPlayerView: PlayerView
    private val gestureOverlayView: View
    private val pbBuffering: ProgressBar
    private val controlsOverlay: View
    private val topBarContainer: View
    private val bottomBarContainer: View
    private val centerControlsContainer: View
    private val btnBack: ImageButton
    private val tvTitle: TextView
    private val tvSubtitle: TextView
    private val tvChapterTitle: TextView
    private val btnCast: ImageButton
    private val btnTrackSelection: ImageButton
    private val btnSpeed: ImageButton
    private val btnLock: ImageButton
    private val btnRewind: ImageButton
    private val btnPlayPause: ImageButton
    private val btnForward: ImageButton
    private val tvCurrentTime: TextView
    private val sbVideoProgress: SeekBar
    private val tvTotalTime: TextView
    private val btnFullscreen: ImageButton
    private val lockedOverlay: View
    private val btnUnlockCenter: ImageButton

    // Action Row Elements
    private val btnLike: ImageButton?
    private val btnDislike: ImageButton?
    private val btnComment: ImageButton?
    private val btnPlaylist: ImageButton?
    private val btnShare: ImageButton?
    private val btnMoreVideos: View?

    // HUD Elements
    private val hudBrightness: LinearLayout
    private val tvBrightnessPercent: TextView
    private val pbBrightness: ProgressBar
    private val hudVolume: LinearLayout
    private val tvVolumePercent: TextView
    private val pbVolume: ProgressBar
    private val hudSeek: LinearLayout
    private val tvSeekDelta: TextView
    private val tvSeekTargetPosition: TextView

    // Internal State
    private var autoPlay = true
    private var enableGestures = true
    private var isFullscreen = false
    private var isUserSeeking = false
    private var isLiked = false
    private var isDisliked = false
    private var currentVideoSource: VideoSource? = null
    private var currentOrientation = PlayerOrientation.LANDSCAPE
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private val handler = Handler(Looper.getMainLooper())

    private var onBackClickListener: OnClickListener? = null
    private var onFullscreenClickListener: ((Boolean) -> Unit)? = null
    private var onShareClickListener: ((VideoSource?) -> Unit)? = null
    private var onMoreVideosClickListener: OnClickListener? = null

    private val hideControlsRunnable = Runnable { hideControls() }
    private val hideHudRunnable = Runnable { hideAllHuds() }

    init {
        LayoutInflater.from(context).inflate(R.layout.view_video_player, this, true)

        exoPlayerView = findViewById(R.id.exoPlayerView)
        gestureOverlayView = findViewById(R.id.gestureOverlayView)
        pbBuffering = findViewById(R.id.pbBuffering)
        controlsOverlay = findViewById(R.id.controlsOverlay)
        topBarContainer = findViewById(R.id.topBarContainer)
        bottomBarContainer = findViewById(R.id.bottomBarContainer)
        centerControlsContainer = findViewById(R.id.centerControlsContainer)
        btnBack = findViewById(R.id.btnBack)
        tvTitle = findViewById(R.id.tvTitle)
        tvSubtitle = findViewById(R.id.tvSubtitle)
        tvChapterTitle = findViewById(R.id.tvChapterTitle)
        btnCast = findViewById(R.id.btnCast)
        btnTrackSelection = findViewById(R.id.btnTrackSelection)
        btnSpeed = findViewById(R.id.btnSpeed)
        btnLock = findViewById(R.id.btnLock)
        btnRewind = findViewById(R.id.btnRewind)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnForward = findViewById(R.id.btnForward)
        tvCurrentTime = findViewById(R.id.tvCurrentTime)
        sbVideoProgress = findViewById(R.id.sbVideoProgress)
        tvTotalTime = findViewById(R.id.tvTotalTime)
        btnFullscreen = findViewById(R.id.btnFullscreen)
        lockedOverlay = findViewById(R.id.lockedOverlay)
        btnUnlockCenter = findViewById(R.id.btnUnlockCenter)

        btnLike = findViewById(R.id.btnLike)
        btnDislike = findViewById(R.id.btnDislike)
        btnComment = findViewById(R.id.btnComment)
        btnPlaylist = findViewById(R.id.btnPlaylist)
        btnShare = findViewById(R.id.btnShare)
        btnMoreVideos = findViewById(R.id.btnMoreVideos)

        hudBrightness = findViewById(R.id.hudBrightness)
        tvBrightnessPercent = findViewById(R.id.tvBrightnessPercent)
        pbBrightness = findViewById(R.id.pbBrightness)
        hudVolume = findViewById(R.id.hudVolume)
        tvVolumePercent = findViewById(R.id.tvVolumePercent)
        pbVolume = findViewById(R.id.pbVolume)
        hudSeek = findViewById(R.id.hudSeek)
        tvSeekDelta = findViewById(R.id.tvSeekDelta)
        tvSeekTargetPosition = findViewById(R.id.tvSeekTargetPosition)

        exoPlayerView.player = repository.exoPlayer

        attrs?.let { parseAttributes(it) }

        setupListeners()
        observeState()
        scheduleControlsAutoHide()
    }

    private fun parseAttributes(attrs: AttributeSet) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.VideoPlayerView)
        autoPlay = typedArray.getBoolean(R.styleable.VideoPlayerView_autoPlay, true)
        val showControls = typedArray.getBoolean(R.styleable.VideoPlayerView_showControls, true)
        enableGestures = typedArray.getBoolean(R.styleable.VideoPlayerView_enableGestures, true)
        val showFastForwardRewind = typedArray.getBoolean(R.styleable.VideoPlayerView_showFastForwardRewind, true)
        val orientationInt = typedArray.getInt(R.styleable.VideoPlayerView_orientation, 1) // default landscape
        val resizeModeInt = typedArray.getInt(R.styleable.VideoPlayerView_resizeMode, 0)

        if (!showControls) {
            controlsOverlay.visibility = GONE
        }
        if (!showFastForwardRewind) {
            btnRewind.visibility = GONE
            btnForward.visibility = GONE
        }

        currentOrientation = when (orientationInt) {
            0 -> PlayerOrientation.PORTRAIT
            1 -> PlayerOrientation.LANDSCAPE
            else -> PlayerOrientation.SENSOR
        }

        val mode = when (resizeModeInt) {
            1 -> AspectRatio.FILL
            2 -> AspectRatio.ZOOM
            3 -> AspectRatio.RATIO_16_9
            4 -> AspectRatio.RATIO_4_3
            else -> AspectRatio.FIT
        }
        setAspectRatioUseCase(mode)

        typedArray.recycle()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListeners() {
        btnPlayPause.setOnClickListener {
            scheduleControlsAutoHide()
            val currentState = getPlayerStateUseCase.playerState.value
            if (currentState is PlayerState.Playing) {
                pauseVideoUseCase()
            } else {
                playVideoUseCase()
            }
        }

        btnRewind.setOnClickListener {
            scheduleControlsAutoHide()
            seekByOffsetUseCase(-10000L)
        }

        btnForward.setOnClickListener {
            scheduleControlsAutoHide()
            seekByOffsetUseCase(10000L)
        }

        btnBack.setOnClickListener { view ->
            if (onBackClickListener != null) {
                onBackClickListener?.onClick(view)
            } else {
                (context as? Activity)?.finish()
            }
        }

        btnFullscreen.setOnClickListener {
            isFullscreen = !isFullscreen
            btnFullscreen.setImageResource(
                if (isFullscreen) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen
            )
            toggleOrientationMode()
            onFullscreenClickListener?.invoke(isFullscreen)
        }

        btnLock.setOnClickListener {
            toggleControlsLockUseCase()
        }

        btnUnlockCenter.setOnClickListener {
            toggleControlsLockUseCase()
        }

        btnCast.setOnClickListener {
            scheduleControlsAutoHide()
            Toast.makeText(context, "Scanning for Cast devices...", Toast.LENGTH_SHORT).show()
        }

        btnLike?.setOnClickListener {
            scheduleControlsAutoHide()
            isLiked = !isLiked
            isDisliked = false
            btnLike.setColorFilter(if (isLiked) 0xFFFF0000.toInt() else 0xFFFFFFFF.toInt())
            btnDislike?.setColorFilter(0xFFFFFFFF.toInt())
            Toast.makeText(context, if (isLiked) "Liked video" else "Removed like", Toast.LENGTH_SHORT).show()
        }

        btnDislike?.setOnClickListener {
            scheduleControlsAutoHide()
            isDisliked = !isDisliked
            isLiked = false
            btnDislike.setColorFilter(if (isDisliked) 0xFFFF0000.toInt() else 0xFFFFFFFF.toInt())
            btnLike?.setColorFilter(0xFFFFFFFF.toInt())
        }

        btnComment?.setOnClickListener {
            scheduleControlsAutoHide()
            Toast.makeText(context, "Opening comments section...", Toast.LENGTH_SHORT).show()
        }

        btnPlaylist?.setOnClickListener {
            scheduleControlsAutoHide()
            Toast.makeText(context, "Saved to your playlist", Toast.LENGTH_SHORT).show()
        }

        btnShare?.setOnClickListener {
            scheduleControlsAutoHide()
            if (onShareClickListener != null) {
                onShareClickListener?.invoke(currentVideoSource)
            } else {
                performDefaultShare()
            }
        }

        btnMoreVideos?.setOnClickListener { view ->
            scheduleControlsAutoHide()
            if (onMoreVideosClickListener != null) {
                onMoreVideosClickListener?.onClick(view)
            } else {
                Toast.makeText(context, "Showing related videos...", Toast.LENGTH_SHORT).show()
            }
        }

        btnSpeed.setOnClickListener {
            scheduleControlsAutoHide()
            PlaybackSpeedDialog.showSpeedDialog(
                context,
                getPlayerStateUseCase.playbackSpeed.value
            ) { speed ->
                setPlaybackSpeedUseCase(speed)
            }
        }

        btnTrackSelection.setOnClickListener {
            scheduleControlsAutoHide()
            val state = getPlayerStateUseCase.trackSelectionState.value
            if (state.audioTracks.isNotEmpty() || state.subtitleTracks.isNotEmpty()) {
                val options = arrayOf("Audio Tracks", "Subtitles")
                AlertDialog.Builder(context)
                    .setTitle("Tracks")
                    .setItems(options) { _, which ->
                        if (which == 0) {
                            TrackSelectionDialog.showAudioTrackDialog(context, state) { trackId ->
                                selectAudioTrackUseCase(trackId)
                            }
                        } else {
                            TrackSelectionDialog.showSubtitleTrackDialog(context, state) { trackId ->
                                selectSubtitleTrackUseCase(trackId)
                            }
                        }
                    }
                    .show()
            } else {
                TrackSelectionDialog.showSubtitleTrackDialog(context, state) { trackId ->
                    selectSubtitleTrackUseCase(trackId)
                }
            }
        }

        sbVideoProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val duration = getPlayerStateUseCase.playbackPosition.value.durationMs
                    if (duration > 0) {
                        val targetMs = (progress.toFloat() / 1000f * duration).toLong()
                        tvCurrentTime.text = formatTime(targetMs)
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
                handler.removeCallbacks(hideControlsRunnable)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    val duration = getPlayerStateUseCase.playbackPosition.value.durationMs
                    if (duration > 0) {
                        val targetMs = (it.progress.toFloat() / 1000f * duration).toLong()
                        seekToUseCase(targetMs)
                    }
                }
                isUserSeeking = false
                scheduleControlsAutoHide()
            }
        })

        if (enableGestures) {
            val gestureListener = PlayerGestureListener(
                context = context,
                viewWidthProvider = { width },
                viewHeightProvider = { height },
                currentPositionProvider = { getPlayerStateUseCase.playbackPosition.value.currentPositionMs },
                durationProvider = { getPlayerStateUseCase.playbackPosition.value.durationMs },
                callbacks = object : PlayerGestureListener.Callbacks {
                    override fun onSingleTap() {
                        toggleControlsVisibility()
                    }

                    override fun onDoubleTapSeek(isForward: Boolean) {
                        val offset = if (isForward) 10000L else -10000L
                        seekByOffsetUseCase(offset)
                        showSeekHud(
                            deltaMs = offset,
                            targetMs = getPlayerStateUseCase.playbackPosition.value.currentPositionMs + offset,
                            durationMs = getPlayerStateUseCase.playbackPosition.value.durationMs
                        )
                    }

                    override fun onBrightnessChanged(percent: Int) {
                        showBrightnessHud(percent)
                    }

                    override fun onVolumeChanged(percent: Int) {
                        showVolumeHud(percent)
                    }

                    override fun onSeekGesture(deltaMs: Long, targetMs: Long) {
                        showSeekHud(
                            deltaMs = deltaMs,
                            targetMs = targetMs,
                            durationMs = getPlayerStateUseCase.playbackPosition.value.durationMs
                        )
                        if (accumulatedSeekComplete(deltaMs)) {
                            seekToUseCase(targetMs)
                        }
                    }

                    override fun onGestureFinished() {
                        scheduleHudHide()
                        scheduleControlsAutoHide()
                    }
                }
            )
            gestureOverlayView.setOnTouchListener(gestureListener)
        } else {
            gestureOverlayView.setOnClickListener {
                toggleControlsVisibility()
            }
        }
    }

    private fun accumulatedSeekComplete(deltaMs: Long): Boolean = deltaMs != 0L

    private fun observeState() {
        getPlayerStateUseCase.playerState.onEach { state ->
            when (state) {
                is PlayerState.Buffering -> pbBuffering.visibility = VISIBLE
                is PlayerState.Playing -> {
                    pbBuffering.visibility = GONE
                    btnPlayPause.setImageResource(R.drawable.ic_pause)
                }
                is PlayerState.Paused -> {
                    pbBuffering.visibility = GONE
                    btnPlayPause.setImageResource(R.drawable.ic_play)
                }
                is PlayerState.Ended -> {
                    pbBuffering.visibility = GONE
                    btnPlayPause.setImageResource(R.drawable.ic_replay)
                    showControls()
                }
                is PlayerState.Idle -> pbBuffering.visibility = GONE
                is PlayerState.Error -> pbBuffering.visibility = GONE
            }
        }.launchIn(coroutineScope)

        getPlayerStateUseCase.playbackPosition.onEach { pos ->
            if (!isUserSeeking) {
                tvCurrentTime.text = formatTime(pos.currentPositionMs)
                tvTotalTime.text = formatTime(pos.durationMs)
                if (pos.durationMs > 0) {
                    val progress = ((pos.currentPositionMs.toFloat() / pos.durationMs.toFloat()) * 1000).toInt()
                    sbVideoProgress.progress = progress
                    sbVideoProgress.secondaryProgress = ((pos.bufferedPositionMs.toFloat() / pos.durationMs.toFloat()) * 1000).toInt()
                }
            }
        }.launchIn(coroutineScope)

        getPlayerStateUseCase.aspectRatio.onEach { ratio ->
            exoPlayerView.resizeMode = when (ratio) {
                AspectRatio.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                AspectRatio.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                AspectRatio.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                AspectRatio.RATIO_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                AspectRatio.RATIO_4_3 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
            }
        }.launchIn(coroutineScope)

        getPlayerStateUseCase.isControlsLocked.onEach { isLocked ->
            if (isLocked) {
                controlsOverlay.visibility = GONE
                lockedOverlay.visibility = VISIBLE
            } else {
                lockedOverlay.visibility = GONE
                showControls()
            }
        }.launchIn(coroutineScope)
    }

    private fun toggleControlsVisibility() {
        if (getPlayerStateUseCase.isControlsLocked.value) return
        if (controlsOverlay.visibility == VISIBLE) {
            hideControls()
        } else {
            showControls()
        }
    }

    fun showControls() {
        if (getPlayerStateUseCase.isControlsLocked.value) return
        controlsOverlay.visibility = VISIBLE
        scheduleControlsAutoHide()
    }

    fun hideControls() {
        controlsOverlay.visibility = GONE
    }

    private fun scheduleControlsAutoHide() {
        handler.removeCallbacks(hideControlsRunnable)
        handler.postDelayed(hideControlsRunnable, 3500)
    }

    private fun showBrightnessHud(percent: Int) {
        hideAllHuds()
        hudBrightness.visibility = VISIBLE
        tvBrightnessPercent.text = "$percent%"
        pbBrightness.progress = percent
    }

    private fun showVolumeHud(percent: Int) {
        hideAllHuds()
        hudVolume.visibility = VISIBLE
        tvVolumePercent.text = "$percent%"
        pbVolume.progress = percent
    }

    private fun showSeekHud(deltaMs: Long, targetMs: Long, durationMs: Long) {
        hideAllHuds()
        hudSeek.visibility = VISIBLE
        val deltaSeconds = deltaMs / 1000
        val sign = if (deltaSeconds >= 0) "+" else ""
        tvSeekDelta.text = "$sign${deltaSeconds}s"
        tvSeekTargetPosition.text = "${formatTime(targetMs)} / ${formatTime(durationMs)}"
    }

    private fun hideAllHuds() {
        hudBrightness.visibility = GONE
        hudVolume.visibility = GONE
        hudSeek.visibility = GONE
    }

    private fun scheduleHudHide() {
        handler.removeCallbacks(hideHudRunnable)
        handler.postDelayed(hideHudRunnable, 1000)
    }

    private fun formatTime(timeMs: Long): String {
        val totalSeconds = (timeMs / 1000).coerceAtLeast(0)
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    private fun toggleOrientationMode() {
        val activity = context as? Activity ?: return
        activity.requestedOrientation = if (isFullscreen) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            when (currentOrientation) {
                PlayerOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                PlayerOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                PlayerOrientation.SENSOR -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            }
        }
    }

    private fun performDefaultShare() {
        val title = currentVideoSource?.title ?: "Video"
        val shareText = when (val source = currentVideoSource) {
            is VideoSource.Network -> "Check out this video: $title\n${source.url}"
            is VideoSource.LocalUri -> "Playing local video: $title\n${source.uri}"
            is VideoSource.FilePath -> "Playing local video: $title"
            else -> "Playing video: $title"
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
    }

    // --- Public Developer APIs ---

    fun setOrientation(orientation: PlayerOrientation) {
        this.currentOrientation = orientation
        val activity = context as? Activity ?: return
        activity.requestedOrientation = when (orientation) {
            PlayerOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            PlayerOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            PlayerOrientation.SENSOR -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }
    }

    fun setVideoTitle(title: String, subtitle: String? = null, chapter: String? = null) {
        tvTitle.text = if (title.endsWith("›")) title else "$title ›"
        if (subtitle != null) {
            tvSubtitle.visibility = VISIBLE
            tvSubtitle.text = subtitle
        } else {
            tvSubtitle.visibility = GONE
        }
        if (chapter != null) {
            tvChapterTitle.visibility = VISIBLE
            tvChapterTitle.text = if (chapter.startsWith("·")) chapter else "· $chapter ›"
        } else {
            tvChapterTitle.visibility = GONE
        }
    }

    fun load(source: VideoSource) {
        this.currentVideoSource = source
        setVideoTitle(source.title ?: "Samsung Galaxy S22, S22+, and S22 Ultra", "GSMArena Official", "Screen and display quality")
        initializePlayerUseCase(source, autoPlay)
    }

    fun loadUrl(url: String, title: String? = null, subtitle: String? = null, chapter: String? = null) {
        val source = VideoSource.Network(url = url, title = title)
        this.currentVideoSource = source
        setVideoTitle(title ?: "Samsung Galaxy S22, S22+, and S22 Ultra", subtitle ?: "GSMArena Official", chapter ?: "Screen and display quality")
        load(source)
    }

    fun loadFile(file: File, title: String? = null) {
        val source = VideoSource.FilePath(path = file.absolutePath, title = title ?: file.name)
        this.currentVideoSource = source
        setVideoTitle(title ?: file.name, "Local Media File", "Storage Video")
        load(source)
    }

    fun loadUri(uri: Uri, title: String? = null) {
        val source = VideoSource.LocalUri(uri = uri, title = title)
        this.currentVideoSource = source
        setVideoTitle(title ?: "Local Media", "Device Storage", "Media Content")
        load(source)
    }

    fun loadRawResource(resId: Int, title: String? = null) {
        val source = VideoSource.RawResource(resId = resId, title = title)
        this.currentVideoSource = source
        setVideoTitle(title ?: "Raw Resource", "App Package", "Embedded Media")
        load(source)
    }

    fun play() = playVideoUseCase()
    fun pause() = pauseVideoUseCase()
    fun seekTo(positionMs: Long) = seekToUseCase(positionMs)
    fun setPlaybackSpeed(speed: Float) = setPlaybackSpeedUseCase(speed)
    fun setAspectRatio(aspectRatio: AspectRatio) = setAspectRatioUseCase(aspectRatio)

    fun setOnBackClickListener(listener: OnClickListener) {
        this.onBackClickListener = listener
    }

    fun setOnFullscreenClickListener(listener: (Boolean) -> Unit) {
        this.onFullscreenClickListener = listener
    }

    fun setOnShareClickListener(listener: (VideoSource?) -> Unit) {
        this.onShareClickListener = listener
    }

    fun setOnMoreVideosClickListener(listener: OnClickListener) {
        this.onMoreVideosClickListener = listener
    }

    fun release() {
        handler.removeCallbacksAndMessages(null)
        repository.release()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        release()
    }
}
