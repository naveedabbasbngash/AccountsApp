package com.mehfooz.accounts.app.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.IMarker
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Lightweight tooltip that shows Day, Credit and Debit together.
 * Assumes dataset index 0 = Debit, 1 = Credit (as we set in the chart code).
 */
class ChartBalloonMarker(
    private val chart: LineChart,
    private val creditLabel: String = "Credit",
    private val debitLabel: String = "Debit",
    textColor: Int = Color.WHITE,
    bgColor: Int = Color.parseColor("#22333B"),
    strokeColor: Int = Color.WHITE
) : IMarker {

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = 32f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = 28f
    }
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = strokeColor
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private var line1 = ""
    private var line2 = ""
    private var line3 = ""

    private val padding = 18f
    private val corner = 18f
    private var bubbleWidth = 0f
    private var bubbleHeight = 0f

    private val offset = MPPointF(0f, 0f)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e == null || chart.data == null) {
            line1 = ""; line2 = ""; line3 = ""
            bubbleWidth = 0f; bubbleHeight = 0f
            return
        }

        val xDay = e.x
        val data = chart.data!!

        fun yAtX(dsIndex: Int): Float {
            if (dsIndex < 0 || dsIndex >= data.dataSetCount) return 0f
            val ds = data.getDataSetByIndex(dsIndex)
            val entry = ds.getEntryForXValue(xDay, Float.NaN)
            return entry?.y ?: 0f
        }

        val debitY  = yAtX(0) // dataset 0 = Debit
        val creditY = yAtX(1) // dataset 1 = Credit

        line1 = "Day ${xDay.roundToInt()}"
        line2 = "$creditLabel: ${formatNum(creditY)}"
        line3 = "$debitLabel:  ${formatNum(debitY)}"

        // measure bubble
        val w1 = titlePaint.measureText(line1)
        val w2 = bodyPaint.measureText(line2)
        val w3 = bodyPaint.measureText(line3)
        val maxW = max(w1, max(w2, w3))

        val h1 = titlePaint.fontMetrics.let { it.bottom - it.top }
        val h2 = bodyPaint.fontMetrics.let { it.bottom - it.top }

        bubbleWidth  = maxW + padding * 2
        bubbleHeight = (h1 + h2 + h2) + padding * 2
    }

    override fun draw(canvas: Canvas, posX: Float, posY: Float) {
        if (bubbleWidth <= 0f || bubbleHeight <= 0f || line1.isEmpty()) return

        val (bx, by) = keepInsideChart(posX, posY)
        val rect = RectF(bx, by, bx + bubbleWidth, by + bubbleHeight)

        canvas.drawRoundRect(rect, corner, corner, bgPaint)
        canvas.drawRoundRect(rect, corner, corner, strokePaint)

        // draw text (baseline math)
        val tFm = titlePaint.fontMetrics
        val bFm = bodyPaint.fontMetrics

        var tx = bx + padding
        var ty = by + padding - tFm.top

        canvas.drawText(line1, tx, ty, titlePaint)
        ty += (bFm.bottom - bFm.top)
        canvas.drawText(line2, tx, ty, bodyPaint)
        ty += (bFm.bottom - bFm.top)
        canvas.drawText(line3, tx, ty, bodyPaint)
    }

    override fun getOffset(): MPPointF = offset

    override fun getOffsetForDrawingAtPoint(posX: Float, posY: Float): MPPointF = offset

    private fun keepInsideChart(posX: Float, posY: Float): Pair<Float, Float> {
        val content = chart.viewPortHandler.contentRect
        var bx = posX
        var by = posY - bubbleHeight - 16f

        if (bx + bubbleWidth > content.right) bx = content.right - bubbleWidth - 4f
        if (bx < content.left) bx = content.left + 4f
        if (by < content.top) by = posY + 16f
        if (by + bubbleHeight > content.bottom) by = content.bottom - bubbleHeight - 4f

        return bx to by
    }

    private fun formatNum(v: Float): String {
        val rounded = (v * 100).roundToInt() / 100f
        return if (rounded % 1f == 0f) rounded.toInt().toString() else String.format("%.2f", rounded)
    }
}