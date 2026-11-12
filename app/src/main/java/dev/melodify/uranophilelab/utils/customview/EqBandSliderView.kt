package dev.melodify.uranophilelab.utils.customview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.roundToInt

class EqBandSliderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var bandIndex: Int = 0
    var minGain: Float = -12f
    var maxGain: Float = 12f
    var currentGain: Float = 0f
        private set

    var onGainChangedListener: ((bandIndex: Int, gainDb: Float) -> Unit)? = null

    private var isDragging = false
    private var lastClickTime: Long = 0L

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38888888")
        style = Paint.Style.FILL
    }

    private val centerTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#77888888")
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private val activeTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F43F5E")
        style = Paint.Style.FILL
    }

    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#888888")
        style = Paint.Style.FILL
    }

    private val thumbBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#121212")
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 21f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val bubbleBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F43F5E")
        style = Paint.Style.FILL
    }

    private val bubbleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 24f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val rectF = RectF()

    fun setGain(gain: Float, notify: Boolean = false) {
        val rounded = gain.coerceIn(minGain, maxGain).roundToInt().toFloat()
        if (rounded != currentGain) {
            currentGain = rounded
            invalidate()
            if (notify) {
                onGainChangedListener?.invoke(bandIndex, currentGain)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val now = System.currentTimeMillis()
                if (now - lastClickTime < 300) {
                    // Double tap detected: reset to 0
                    setGain(0f, notify = true)
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    isDragging = false
                    lastClickTime = 0L
                    invalidate()
                    return true
                }
                lastClickTime = now
                isDragging = true
                parent.requestDisallowInterceptTouchEvent(true)
                updateGainFromTouch(event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                isDragging = true
                parent.requestDisallowInterceptTouchEvent(true)
                updateGainFromTouch(event.y)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                parent.requestDisallowInterceptTouchEvent(false)
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateGainFromTouch(y: Float) {
        val h = height.toFloat()
        if (h <= 0) return

        val padding = 24f
        val usableH = (h - 2 * padding).coerceAtLeast(1f)
        val clampedY = (y - padding).coerceIn(0f, usableH)

        val fraction = 1f - (clampedY / usableH)
        val calculatedGain = (minGain + fraction * (maxGain - minGain)).roundToInt().toFloat()

        if (calculatedGain != currentGain) {
            if (calculatedGain == 0f || currentGain == 0f) {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            } else {
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }
            currentGain = calculatedGain
            invalidate()
            onGainChangedListener?.invoke(bandIndex, currentGain)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val cx = w / 2f
        val padding = 24f
        val usableH = (h - 2 * padding).coerceAtLeast(1f)

        fun gainToY(g: Float): Float {
            val fraction = 1f - ((g - minGain) / (maxGain - minGain))
            return padding + fraction * usableH
        }

        val trackWidth = 12f
        // Inactive track
        rectF.set(cx - trackWidth / 2f, padding, cx + trackWidth / 2f, h - padding)
        canvas.drawRoundRect(rectF, trackWidth / 2f, trackWidth / 2f, trackPaint)

        val centerY = gainToY(0f)

        // Center tick at 0 dB
        canvas.drawLine(cx - 16f, centerY, cx + 16f, centerY, centerTickPaint)

        // Active fill
        val thumbY = gainToY(currentGain)
        val fillTop = Math.min(centerY, thumbY)
        val fillBottom = Math.max(centerY, thumbY)

        val isChanged = abs(currentGain) > 0.1f
        if (isChanged && isEnabled) {
            rectF.set(cx - trackWidth / 2f, fillTop, cx + trackWidth / 2f, fillBottom)
            canvas.drawRoundRect(rectF, trackWidth / 2f, trackWidth / 2f, activeTrackPaint)
        }

        // Thumb styling: neutral gray at 0 dB, accent red when changed
        thumbPaint.color = if (isChanged && isEnabled) Color.parseColor("#F43F5E") else Color.parseColor("#999999")

        val thumbWidth = (w * 0.88f).coerceIn(38f, 62f)
        val thumbHeight = 32f
        val thumbTop = (thumbY - thumbHeight / 2f).coerceIn(padding / 2f, h - padding / 2f - thumbHeight)

        rectF.set(cx - thumbWidth / 2f, thumbTop, cx + thumbWidth / 2f, thumbTop + thumbHeight)

        val currentAlpha = if (isEnabled) 255 else 90
        thumbPaint.alpha = currentAlpha
        activeTrackPaint.alpha = currentAlpha

        canvas.drawRoundRect(rectF, 14f, 14f, thumbPaint)
        canvas.drawRoundRect(rectF, 14f, 14f, thumbBorderPaint)

        // Gain value text on thumb
        val gainInt = currentGain.roundToInt()
        val textStr = when {
            gainInt > 0 -> "+$gainInt"
            else -> "$gainInt"
        }
        val textY = rectF.centerY() - ((textPaint.descent() + textPaint.ascent()) / 2f)
        textPaint.alpha = if (isEnabled) 255 else 120
        canvas.drawText(textStr, cx, textY, textPaint)

        // Value Bubble above finger while dragging
        if (isDragging && isEnabled) {
            val bubbleText = if (gainInt > 0) "+$gainInt dB" else "$gainInt dB"
            val bubbleWidth = 68f
            val bubbleHeight = 36f
            val bubbleTop = (thumbTop - bubbleHeight - 12f).coerceIn(0f, h - bubbleHeight)

            rectF.set(cx - bubbleWidth / 2f, bubbleTop, cx + bubbleWidth / 2f, bubbleTop + bubbleHeight)
            canvas.drawRoundRect(rectF, 12f, 12f, bubbleBgPaint)

            val bubbleTextY = rectF.centerY() - ((bubbleTextPaint.descent() + bubbleTextPaint.ascent()) / 2f)
            canvas.drawText(bubbleText, cx, bubbleTextY, bubbleTextPaint)
        }
    }
}
