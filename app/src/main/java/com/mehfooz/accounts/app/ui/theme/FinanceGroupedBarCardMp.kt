package com.mehfooz.accounts.app.ui

import android.graphics.Color
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
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.mehfooz.accounts.app.model.DayValue
import kotlin.math.max

/**
 * Grouped Bar Chart (Credits vs Debits per day)
 * - Credits = green
 * - Debits  = red
 * - Uses DayValue(day: Int, amountUnits: Float)
 */
@Composable
fun FinanceGroupedBarCardMp(
    title: String = "Credits vs Debits (Grouped Bars)",
    credits: List<DayValue>,
    debits: List<DayValue>,
) {
    val darkBlue = remember { Color.parseColor("#0B1E3A") }
    val green    = remember { Color.parseColor("#35D07F") } // credit
    val red      = remember { Color.parseColor("#FF5A6E") } // debit
    val white70  = remember { Color.argb((0.70f * 255).toInt(), 255, 255, 255) }
    val gridCol  = remember { Color.argb((0.20f * 255).toInt(), 255, 255, 255) }

    // Prepare x-days (union) so both series align, fill missing with 0
    val days: List<Int> = run {
        val s = (credits.map { it.day } + debits.map { it.day }).toSortedSet()
        if (s.isEmpty()) emptyList() else s.toList()
    }

    // Build maps for quick lookup
    val crMap = credits.associateBy({ it.day }, { it.amountUnits })
    val drMap = debits.associateBy({ it.day }, { it.amountUnits })

    // Convert to BarEntries (x = day as float, y = amount)
    val creditEntries = days.map { d -> BarEntry(d.toFloat(), (crMap[d] ?: 0f)) }
    val debitEntries  = days.map { d -> BarEntry(d.toFloat(), (drMap[d] ?: 0f)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(darkBlue))
    ) {
        Column(Modifier.fillMaxWidth()) {
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
                    .height(300.dp),
                factory = { ctx ->
                    BarChart(ctx).apply {
                        setBackgroundColor(darkBlue)
                        description.isEnabled = false
                        setNoDataText("No data")
                        setNoDataTextColor(white70)
                        setDrawGridBackground(false)
                        setScaleEnabled(true)
                        setPinchZoom(false)

                        legend.apply {
                            isEnabled = true
                            textColor = white70
                            form = Legend.LegendForm.SQUARE
                            xEntrySpace = 12f
                        }

                        axisRight.isEnabled = false
                        axisLeft.apply {
                            textColor = white70
                            gridColor = gridCol
                            axisMinimum = 0f
                            setDrawAxisLine(false)
                            setDrawGridLines(true)
                        }

                        xAxis.apply {
                            position = XAxis.XAxisPosition.BOTTOM
                            textColor = white70
                            gridColor = gridCol
                            setDrawAxisLine(false)
                            setDrawGridLines(true)
                            granularity = 1f
                            setCenterAxisLabels(true)
                            valueFormatter = object : ValueFormatter() {
                                override fun getFormattedValue(value: Float): String {
                                    // Show integer day labels without decimals
                                    return value.toInt().toString()
                                }
                            }
                        }

                        setViewPortOffsets(24f, 16f, 24f, 32f)
                    }
                },
                update = { chart ->
                    if (days.isEmpty()) {
                        chart.data = null
                        chart.invalidate()
                        return@AndroidView
                    }

                    val creditSet = BarDataSet(creditEntries, "Credit").apply {
                        color = green
                        setDrawValues(false)
                        axisDependency = YAxis.AxisDependency.LEFT
                    }
                    val debitSet = BarDataSet(debitEntries, "Debit").apply {
                        color = red
                        setDrawValues(false)
                        axisDependency = YAxis.AxisDependency.LEFT
                    }

                    val barData = BarData(creditSet, debitSet)

                    // Grouping math (2 datasets):
                    // total group width should be 1.0 for neat "one day per step":
                    // groupWidth = groupSpace + 2*barSpace + 2*barWidth  == 1.0
                    val barWidth  = 0.4f
                    val barSpace  = 0.05f
                    val groupSpace = 0.10f
                    barData.barWidth = barWidth

                    chart.data = barData

                    // MPAndroidChart expects a contiguous x-range to group.
                    // We’ll start grouping at minDay as startX.
                    val minDay = days.first().toFloat()
                    val count  = days.size
                    val groupWidth = barData.getGroupWidth(groupSpace, barSpace)

                    chart.xAxis.axisMinimum = minDay
                    chart.xAxis.axisMaximum = minDay + groupWidth * count
                    chart.groupBars(minDay, groupSpace, barSpace)

                    // Optional: zoom to show many days nicely
                    chart.setVisibleXRangeMaximum(max(7f, count.toFloat()))

                    chart.invalidate()
                    chart.animateY(700)
                }
            )
        }
    }
}