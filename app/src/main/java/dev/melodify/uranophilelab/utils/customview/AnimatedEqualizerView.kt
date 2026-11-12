package dev.melodify.uranophilelab.utils.customview

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

/**
 * Modern animated equalizer wave indicator (Echo & Auris style)
 * Animates 3 smooth vertical audio bars when music is actively playing.
 */
class AnimatedEqualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#1DB954") // Default Spotify green
    }

    private val barRect = RectF()
    private val barCount = 3
    private val barHeights = floatArrayOf(0.3f, 0.6f, 0.4f)
    private val animators = mutableListOf<ValueAnimator>()

    var barColor: Int
        get() = paint.color
        set(value) {
            paint.color = value
            invalidate()
        }

    var isAnimating: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    startAnimators()
                } else {
                    stopAnimators()
                }
            }
        }

    init {
        setupAnimators()
    }

    private fun setupAnimators() {
        val durations = longArrayOf(450L, 620L, 520L)
        val minHeights = floatArrayOf(0.18f, 0.25f, 0.18f)
        val maxHeights = floatArrayOf(0.85f, 1.0f, 0.82f)

        for (i in 0 until barCount) {
            val animator = ValueAnimator.ofFloat(minHeights[i], maxHeights[i]).apply {
                duration = durations[i]
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { va ->
                    barHeights[i] = va.animatedValue as Float
                    invalidate()
                }
            }
            animators.add(animator)
        }
    }

    private fun startAnimators() {
        animators.forEach { animator ->
            if (!animator.isRunning) {
                animator.start()
            }
        }
    }

    private fun stopAnimators() {
        animators.forEach { it.cancel() }
        barHeights[0] = 0.25f
        barHeights[1] = 0.35f
        barHeights[2] = 0.25f
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isAnimating) {
            startAnimators()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animators.forEach { it.cancel() }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val spacing = w * 0.16f
        val totalSpacing = (barCount - 1) * spacing
        val barWidth = (w - totalSpacing) / barCount
        val cornerRadius = barWidth / 2f

        var currentLeft = 0f
        for (i in 0 until barCount) {
            val currentBarH = (h * barHeights[i]).coerceAtLeast(barWidth)
            val top = h - currentBarH
            val right = currentLeft + barWidth
            barRect.set(currentLeft, top, right, h)
            canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, paint)
            currentLeft += barWidth + spacing
        }
    }
}
