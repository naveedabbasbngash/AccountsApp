package com.mehfooz.accounts.app.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.mehfooz.accounts.app.model.DayValue
import kotlin.math.round

@Composable
fun FinanceChartCardMp(
    title: String = "Credits vs Debits",
    credits: List<DayValue>,
    debits: List<DayValue>,
) {
    val darkBlue = remember { Color.parseColor("#0B1E3A") }
    val green    = remember { Color.parseColor("#35D07F") } // credit
    val red      = remember { Color.parseColor("#FF5A6E") } // debit
    val white100 = remember { Color.WHITE }
    val gridCol  = remember { Color.argb((0.28f * 255).toInt(), 255, 255, 255) } // a bit brighter

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(darkBlue))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )

            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                factory = { ctx ->
                    LineChart(ctx).apply {
                        setBackgroundColor(darkBlue)
                        description.isEnabled = false
                        setNoDataText("No data")
                        setNoDataTextColor(white100)

                        // LEGEND — make it clearly visible and outside the plot
                        legend.apply {
                            isEnabled = true
                            textColor = white100
                            textSize = 12f
                            form = Legend.LegendForm.LINE
                            xEntrySpace = 12f
                            verticalAlignment = Legend.LegendVerticalAlignment.TOP
                            horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
                            orientation = Legend.LegendOrientation.HORIZONTAL
                            setDrawInside(false)
                        }

                        // RIGHT AXIS OFF
                        axisRight.isEnabled = false

                        // LEFT AXIS — brighter/grid + bigger labels
                        axisLeft.apply {
                            textColor = white100
                            textSize = 11f
                            gridColor = gridCol
                            axisLineColor = white100
                            setDrawAxisLine(true)
                            setDrawGridLines(true)
                            setLabelCount(6, /*force*/false)
                        }

                        // X AXIS — bottom, clearer labels
                        xAxis.apply {
                            position = XAxis.XAxisPosition.BOTTOM
                            textColor = white100
                            textSize = 11f
                            gridColor = gridCol
                            axisLineColor = white100
                            setDrawAxisLine(true)
                            setDrawGridLines(true)
                            labelCount = 6
                            granularity = 1f
                        }

                        // Space so legend/labels don’t clip
                        setViewPortOffsets(36f, 18f, 24f, 32f)

                        // Interactions/highlights
                        setTouchEnabled(true)
                        isDragEnabled = true
                        setScaleEnabled(false)
                        setPinchZoom(false)
                        isHighlightPerTapEnabled = true
                        isHighlightPerDragEnabled = true
                    }
                },
                update = { chart ->
                    // Build entries
                    val creditEntries = credits.sortedBy { it.day }.map { Entry(it.day.toFloat(), it.amountUnits) }
                    val debitEntries  = debits.sortedBy { it.day }.map { Entry(it.day.toFloat(), it.amountUnits) }

                    // Value formatter: show 0/00 decimals nicely
                    val vf = object : ValueFormatter() {
                        override fun getPointLabel(e: Entry?): String {
                            if (e == null) return ""
                            // show up to 2 decimals, trim .00
                            val v = round(e.y * 100f) / 100f
                            return if (v % 1f == 0f) v.toInt().toString() else "%.2f".format(v)
                        }
                    }

                    // === DEBIT (red, solid, with soft fill) ===
                    val debitSet = LineDataSet(debitEntries, "Debit").apply {
                        color = red                 // fully opaque
                        lineWidth = 3.0f
                        mode = LineDataSet.Mode.CUBIC_BEZIER

                        // Circles
                        setDrawCircles(true)
                        circleRadius = 4.0f
                        setCircleColor(red)
                        setDrawCircleHole(true)
                        circleHoleRadius = 2.2f
                        setCircleHoleColor(Color.WHITE)

                        // Fill
                        setDrawFilled(true)
                        fillDrawable = verticalFade(red, darkBlue)
                        fillAlpha = 120 // stronger fill

                        // Values on points (only when highlighted we’ll show via highlight+labels)
                        setDrawValues(false)

                        // Highlight line for taps/drags
                        isHighlightEnabled = true
                        highLightColor = Color.WHITE
                        highlightLineWidth = 1.2f
                        setDrawHorizontalHighlightIndicator(false)
                        setDrawVerticalHighlightIndicator(true)
                    }

                    // === CREDIT (green, solid, no fill so red remains visible) ===
                    val creditSet = LineDataSet(creditEntries, "Credit").apply {
                        color = green               // fully opaque
                        lineWidth = 3.0f
                        mode = LineDataSet.Mode.CUBIC_BEZIER

                        setDrawCircles(true)
                        circleRadius = 4.0f
                        setCircleColor(green)
                        setDrawCircleHole(true)
                        circleHoleRadius = 2.0f
                        setCircleHoleColor(Color.WHITE)

                        setDrawFilled(false)
                        setDrawValues(false)

                        isHighlightEnabled = true
                        highLightColor = Color.WHITE
                        highlightLineWidth = 1.2f
                        setDrawHorizontalHighlightIndicator(false)
                        setDrawVerticalHighlightIndicator(true)
                    }

                    // OPTIONAL: show value labels when a point is highlighted (without XML marker).
                    // We emulate this by temporarily enabling drawValues while highlighted.
                    // Simpler: keep labels always on (uncomment next 3 lines) if you prefer.
                    // debitSet.setDrawValues(true)
                    // creditSet.setDrawValues(true)
                    // debitSet.valueTextColor = Color.WHITE; creditSet.valueTextColor = Color.WHITE

                    // Attach a value formatter (applies if you turn drawValues on)
                    debitSet.valueFormatter = vf
                    creditSet.valueFormatter = vf
                    debitSet.valueTextColor = Color.WHITE
                    creditSet.valueTextColor = Color.WHITE
                    debitSet.valueTextSize = 10f
                    creditSet.valueTextSize = 10f

                    // Order bottom first
                    chart.data = LineData(debitSet, creditSet)

                    // Better highlight feedback: when you tap, we enable values on the fly

                    // Order matters: bottom first, then top
                    chart.data = LineData(debitSet, creditSet)

// <<< ADD THIS: one tooltip that shows both Credit & Debit for the tapped day >>>

// Anim + redraw
                    chart.animateX(700)
                    chart.invalidate()
                }
            )
        }
    }
}

private fun verticalFade(topColor: Int, bottomColor: Int): GradientDrawable =
    GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(
            Color.argb((0.45f * 255).toInt(), Color.red(topColor), Color.green(topColor), Color.blue(topColor)),
            Color.argb(0, Color.red(bottomColor), Color.green(bottomColor), Color.blue(bottomColor))
        )
    ).apply { cornerRadius = 0f }