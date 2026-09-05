package androidx.multimedia.video.ui.view

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class PlayerGestureListener(
    private val context: Context,
    private val viewWidthProvider: () -> Int,
    private val viewHeightProvider: () -> Int,
    private val currentPositionProvider: () -> Long,
    private val durationProvider: () -> Long,
    private val callbacks: Callbacks
) : View.OnTouchListener {

    interface Callbacks {
        fun onSingleTap()
        fun onDoubleTapSeek(isForward: Boolean)
        fun onBrightnessChanged(percent: Int)
        fun onVolumeChanged(percent: Int)
        fun onSeekGesture(deltaMs: Long, targetMs: Long)
        fun onGestureFinished()
    }

    private enum class ScrollMode { NONE, BRIGHTNESS, VOLUME, SEEK }

    private var activeScrollMode = ScrollMode.NONE
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var initialVolume = 0
    private val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    private var initialBrightness = 0.5f
    private var initialSeekPosition = 0L
    private var accumulatedSeekDeltaMs = 0L

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            callbacks.onSingleTap()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            val width = viewWidthProvider()
            if (width > 0) {
                val isForward = e.x > width / 2
                callbacks.onDoubleTapSeek(isForward)
            }
            return true
        }

        override fun onDown(e: MotionEvent): Boolean {
            activeScrollMode = ScrollMode.NONE
            initialVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            initialBrightness = getWindowBrightness()
            initialSeekPosition = currentPositionProvider()
            accumulatedSeekDeltaMs = 0L
            return true
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (e1 == null) return false

            val width = viewWidthProvider()
            val height = viewHeightProvider()
            if (width <= 0 || height <= 0) return false

            val deltaX = e2.x - e1.x
            val deltaY = e1.y - e2.y

            if (activeScrollMode == ScrollMode.NONE) {
                if (abs(deltaX) > abs(deltaY) && abs(deltaX) > 20) {
                    activeScrollMode = ScrollMode.SEEK
                } else if (abs(deltaY) > abs(deltaX) && abs(deltaY) > 20) {
                    activeScrollMode = if (e1.x < width * 0.4f) {
                        ScrollMode.BRIGHTNESS
                    } else if (e1.x > width * 0.6f) {
                        ScrollMode.VOLUME
                    } else {
                        ScrollMode.NONE
                    }
                }
            }

            when (activeScrollMode) {
                ScrollMode.BRIGHTNESS -> {
                    val percentDelta = (deltaY / height) * 1.5f
                    val newBrightness = (initialBrightness + percentDelta).coerceIn(0.01f, 1.0f)
                    setWindowBrightness(newBrightness)
                    val percentInt = (newBrightness * 100).toInt()
                    callbacks.onBrightnessChanged(percentInt)
                }
                ScrollMode.VOLUME -> {
                    val volumeDelta = (deltaY / height) * maxVolume * 1.5f
                    val newVolume = (initialVolume + volumeDelta.toInt()).coerceIn(0, maxVolume)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                    val percentInt = ((newVolume.toFloat() / maxVolume.toFloat()) * 100).toInt()
                    callbacks.onVolumeChanged(percentInt)
                }
                ScrollMode.SEEK -> {
                    val duration = durationProvider()
                    if (duration > 0) {
                        val seekRatio = deltaX / width.toFloat()
                        // Full swipe across screen = 90 seconds seek
                        accumulatedSeekDeltaMs = (seekRatio * 90000L).toLong()
                        val targetMs = (initialSeekPosition + accumulatedSeekDeltaMs).coerceIn(0L, duration)
                        callbacks.onSeekGesture(accumulatedSeekDeltaMs, targetMs)
                    }
                }
                ScrollMode.NONE -> {}
            }
            return true
        }
    })

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (event == null) return false
        val handled = gestureDetector.onTouchEvent(event)

        if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            if (activeScrollMode == ScrollMode.SEEK && accumulatedSeekDeltaMs != 0L) {
                val duration = durationProvider()
                val targetMs = (initialSeekPosition + accumulatedSeekDeltaMs).coerceIn(0L, duration)
                callbacks.onSeekGesture(accumulatedSeekDeltaMs, targetMs)
            }
            activeScrollMode = ScrollMode.NONE
            callbacks.onGestureFinished()
        }
        return handled
    }

    private fun getWindowBrightness(): Float {
        val activity = context as? Activity ?: return 0.5f
        val lp = activity.window.attributes
        return if (lp.screenBrightness < 0) 0.5f else lp.screenBrightness
    }

    private fun setWindowBrightness(brightness: Float) {
        val activity = context as? Activity ?: return
        val lp = activity.window.attributes
        lp.screenBrightness = brightness
        activity.window.attributes = lp
    }
}
