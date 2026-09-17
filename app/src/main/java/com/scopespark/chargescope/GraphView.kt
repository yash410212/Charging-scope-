package com.scopespark.chargescope

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

class GraphView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    private val values = ArrayDeque<Float>()
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(99,215,255); strokeWidth = 5f; style = Paint.Style.STROKE
    }
    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(55,65,75); strokeWidth = 1f
    }
    fun add(v: Float) {
        if (v.isNaN() || v <= 0f) return
        values.addLast(v)
        while (values.size > 60) values.removeFirst()
        invalidate()
    }
    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat(); val h = height.toFloat()
        for (i in 1..3) c.drawLine(0f, h*i/4f, w, h*i/4f, grid)
        if (values.size < 2) return
        val minV = values.minOrNull() ?: 0f
        val maxV = max(values.maxOrNull() ?: 1f, minV + 0.1f)
        val p = Path()
        values.forEachIndexed { i, v ->
            val x = i * w / max(1, values.size - 1)
            val y = h - ((v - minV) / (maxV - minV)) * (h - 14f) - 7f
            if (i == 0) p.moveTo(x,y) else p.lineTo(x,y)
        }
        c.drawPath(p, line)
    }
}
