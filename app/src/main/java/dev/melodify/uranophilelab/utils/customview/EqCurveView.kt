package dev.melodify.uranophilelab.utils.customview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

class EqCurveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var gains: FloatArray = FloatArray(10)
    private var isSelectedState: Boolean = false
    private var accentColor: Int = Color.parseColor("#F43F5E")

    private val baselinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33888888")
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val curvePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        strokeWidth = 4f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val path = Path()
    private val fillPath = Path()
    private var cachedShaderHeight: Float = -1f
    private var cachedShaderColor: Int = -1

    fun setGains(newGains: FloatArray, isSelected: Boolean, accentColor: Int = Color.parseColor("#F43F5E")) {
        this.gains = newGains
        this.isSelectedState = isSelected
        this.accentColor = accentColor
        this.curvePaint.color = if (isSelected) accentColor else Color.parseColor("#888888")
        this.curvePaint.strokeWidth = if (isSelected) 5f else 3.5f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0 || gains.size < 2) return

        val midY = h / 2f

        // Draw 0 dB reference line
        canvas.drawLine(0f, midY, w, midY, baselinePaint)

        val dx = w / (gains.size - 1)
        val amplitude = (h / 2f) - 6f // inset padding

        fun pointX(i: Int) = dx * i
        fun pointY(i: Int): Float {
            val clampedGain = gains[i].coerceIn(-12f, 12f)
            return midY - (clampedGain / 12f) * amplitude
        }

        path.reset()
        path.moveTo(pointX(0), pointY(0))

        for (i in 0 until gains.size - 1) {
            val px = pointX(i)
            val py = pointY(i)
            val qx = pointX(i + 1)
            val qy = pointY(i + 1)

            val cp1x = px + dx / 2f
            val cp1y = py
            val cp2x = qx - dx / 2f
            val cp2y = qy

            path.cubicTo(cp1x, cp1y, cp2x, cp2y, qx, qy)
        }

        // Fill area under curve down to baseline
        fillPath.reset()
        fillPath.addPath(path)
        fillPath.lineTo(w, midY)
        fillPath.lineTo(0f, midY)
        fillPath.close()

        val topAlphaColor = if (isSelectedState) {
            Color.argb(70, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
        } else {
            Color.argb(25, 136, 136, 136)
        }

        if (cachedShaderHeight != h || cachedShaderColor != topAlphaColor) {
            cachedShaderHeight = h
            cachedShaderColor = topAlphaColor
            fillPaint.shader = LinearGradient(
                0f, 0f, 0f, h,
                topAlphaColor, Color.argb(0, 0, 0, 0),
                Shader.TileMode.CLAMP
            )
        }

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(path, curvePaint)
    }
}
