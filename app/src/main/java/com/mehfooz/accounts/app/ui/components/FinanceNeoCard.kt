package com.mehfooz.accounts.app.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.MPPointF
import com.mehfooz.accounts.app.model.DayValue
import kotlin.math.roundToInt

@Composable
fun FinanceNeoCard(
    selectedTab: MonthTab,
    onTabChange: (MonthTab) -> Unit,
    credits: List<DayValue>,
    debits: List<DayValue>,
    currency: String = "€",
    edgeToEdge: Boolean = false, // NEW: when true, remove side padding & card chrome
) {
    // Colors
    val dark     = Color.parseColor("#0B1E3A")
    val darkTop  = Color.parseColor("#0E274E")
    val green    = Color.parseColor("#35D07F")
    val red      = Color.parseColor("#FF5A6E")
    val white60  = Color.argb((0.60f * 255).toInt(), 255,255,255)
    val grid     = Color.argb((0.15f * 255).toInt(), 255,255,255)

    // month totals
    val monthCredit = remember(credits) { credits.sumOf { it.amountUnits.toDouble() } }
    val monthDebit  = remember(debits ) { debits.sumOf  { it.amountUnits.toDouble() } }

    // selected day (tap)
    var selectedDay by remember { mutableStateOf<Int?>(null) }
    var selectedCredit by remember { mutableStateOf(0.0) }
    var selectedDebit  by remember { mutableStateOf(0.0) }

    // for fast lookup on tap
    val creditByDay = remember(credits) { credits.associate { it.day to it.amountUnits.toDouble() } }
    val debitByDay  = remember(debits ) { debits.associate  { it.day to it.amountUnits.toDouble() } }

    // container (gradient card or transparent when edge-to-edge)
    val containerModifier = Modifier
        .fillMaxWidth()
        .let { m -> if (edgeToEdge) m else m.clip(RoundedCornerShape(28.dp)) }
        .background(
            brush = Brush.verticalGradient(
                listOf(
                    androidx.compose.ui.graphics.Color(darkTop),
                    androidx.compose.ui.graphics.Color(dark)
                )
            )
        )
        .let { m -> if (edgeToEdge) m else m.padding(horizontal = 16.dp) }
        .padding(horizontal = if (edgeToEdge) 0.dp else 16.dp, vertical = 18.dp)

    Box(modifier = containerModifier) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // segmented pill
            SegmentedTwoTabs(
                left = "This month",
                right = "Last month",
                selectedLeft = selectedTab == MonthTab.THIS_MONTH,
                onSelect = { isLeft ->
                    selectedDay = null
                    onTabChange(if (isLeft) MonthTab.THIS_MONTH else MonthTab.LAST_MONTH)
                }
            )

            // stacked center values (Credit green, Debit red)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

            }

            // chart
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp),
                factory = { ctx ->
                    LineChart(ctx).apply {
                        setBackgroundColor(Color.TRANSPARENT)
                        description.isEnabled = false
                        setTouchEnabled(true)
                        isDragEnabled = true
                        setScaleEnabled(false)
                        isHighlightPerTapEnabled = true
                        setPinchZoom(false)

                        // very small side offsets when edge-to-edge
                        setViewPortOffsets(
                            if (edgeToEdge) 8f else 16f,
                            12f,
                            if (edgeToEdge) 8f else 16f,
                            18f
                        )

                        legend.apply {
                            isEnabled = false // turn true if you want labels
                            textColor = white60
                            form = Legend.LegendForm.LINE
                        }

                        axisRight.isEnabled = false

                        axisLeft.apply {
                            textColor = white60
                            gridColor = grid
                            setDrawAxisLine(false)
                            setDrawGridLines(true)
                            setLabelCount(4, false)
                        }

                        xAxis.apply {
                            position = XAxis.XAxisPosition.BOTTOM
                            textColor = white60
                            gridColor = grid
                            setDrawAxisLine(false)
                            setDrawGridLines(true)
                            granularity = 1f
                            setLabelCount(6, false)
                        }

                        isHighlightPerTapEnabled = true
                        setDrawMarkers(true)
                    }
                },
                update = { chart ->
                    val creditEntries = credits.sortedBy { it.day }.map { Entry(it.day.toFloat(), it.amountUnits) }
                    val debitEntries  = debits.sortedBy  { it.day }.map { Entry(it.day.toFloat(), it.amountUnits) }

                    val creditSet = LineDataSet(creditEntries, "Credit (Jama)").apply {
                        val gA = Color.argb(190, Color.red(green), Color.green(green), Color.blue(green))
                        color = gA
                        lineWidth = 2.8f
                        mode = LineDataSet.Mode.CUBIC_BEZIER
                        setDrawValues(false)
                        setDrawCircles(true)
                        circleRadius = 3.6f
                        setCircleColor(gA)
                        setDrawCircleHole(true)
                        circleHoleRadius = 2.0f
                        setCircleHoleColor(Color.WHITE)
                        setDrawFilled(false)
                        highLightColor = Color.argb(160, 255,255,255)
                    }

                    val debitSet = LineDataSet(debitEntries, "Debit (Banam)").apply {
                        color = red
                        lineWidth = 3.0f
                        mode = LineDataSet.Mode.CUBIC_BEZIER
                        setDrawValues(false)
                        setDrawCircles(true)
                        circleRadius = 4.0f
                        setCircleColor(red)
                        setDrawCircleHole(true)
                        circleHoleRadius = 2.2f
                        setCircleHoleColor(Color.WHITE)
                        setDrawFilled(true)
                        fillDrawable = verticalFade(red, Color.TRANSPARENT)
                        fillAlpha = 90
                        enableDashedLine(10f, 6f, 0f)
                        highLightColor = Color.argb(160, 255,255,255)
                    }

                    chart.data = LineData(debitSet, creditSet)
                    chart.marker = ChartBalloonMarker(
                        chart = chart,
                        creditLabel = "Credit (Jama)",
                        debitLabel = "Debit (Banam)"
                    )

                    chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                        override fun onValueSelected(e: Entry?, h: Highlight?) {
                            val day = e?.x?.roundToInt() ?: return
                            selectedDay = day
                            selectedCredit = creditByDay[day] ?: 0.0
                            selectedDebit  = debitByDay[day]  ?: 0.0
                        }
                        override fun onNothingSelected() { selectedDay = null }
                    })

                    chart.animateX(700)
                    chart.invalidate()
                }
            )
        }
    }
}

/* ---------- segmented two-tab pill ---------- */
@Composable
private fun SegmentedTwoTabs(
    left: String,
    right: String,
    selectedLeft: Boolean,
    onSelect: (left: Boolean) -> Unit
) {
    val pill = RoundedCornerShape(999.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(top = 2.dp)
            .clip(pill)
            .background(androidx.compose.ui.graphics.Color(0x1FFFFFFF))
            .padding(4.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        val selCol = androidx.compose.ui.graphics.Color.White
        val unsCol = androidx.compose.ui.graphics.Color(0x99FFFFFF)

        TextButton(
            modifier = Modifier
                .weight(1f)
                .clip(pill)
                .background(if (selectedLeft) selCol else androidx.compose.ui.graphics.Color.Transparent),
            onClick = { onSelect(true) },
            contentPadding = PaddingValues(vertical = 6.dp)
        ) { Text(left, color = if (selectedLeft) androidx.compose.ui.graphics.Color(0xFF0B1E3A) else unsCol) }

        Spacer(Modifier.width(6.dp))

        TextButton(
            modifier = Modifier
                .weight(1f)
                .clip(pill)
                .background(if (!selectedLeft) selCol else androidx.compose.ui.graphics.Color.Transparent),
            onClick = { onSelect(false) },
            contentPadding = PaddingValues(vertical = 6.dp)
        ) { Text(right, color = if (!selectedLeft) androidx.compose.ui.graphics.Color(0xFF0B1E3A) else unsCol) }
    }
}

/* ---------- marker that shows Credit & Debit together ---------- */
private class BalloonMarker(
    chart: LineChart,
    private val creditLabel: String,
    private val debitLabel: String,
    private val currency: String,
    private val green: Int,
    private val red: Int
) : MarkerView(chart.context, android.R.layout.simple_list_item_2) {

    // precompute maps from the chart’s current data
    private val crMap: Map<Int, Double>
    private val drMap: Map<Int, Double>

    init {
        val cr = mutableMapOf<Int, Double>()
        val dr = mutableMapOf<Int, Double>()
        chart.data?.dataSets?.forEach { ds ->
            val isCredit = ds.label.equals("Credit (Jama)", ignoreCase = true)
            for (i in 0 until ds.entryCount) {
                val e = ds.getEntryForIndex(i)
                val day = e.x.toInt()
                val v = e.y.toDouble()
                if (isCredit) cr[day] = v else dr[day] = v
            }
        }
        crMap = cr
        drMap = dr
    }

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        // using the built-in simple_list_item_2:
        val title = findViewById<android.widget.TextView>(android.R.id.text1)
        val body  = findViewById<android.widget.TextView>(android.R.id.text2)

        val day = e?.x?.roundToInt() ?: 0
        val credit = crMap[day] ?: 0.0
        val debit  = drMap[day] ?: 0.0

        title.text = "Day $day"
        title.setTextColor(Color.WHITE)
        title.textSize = 14f

        val crTxt = "$creditLabel: $currency ${formatMoney(credit)}"
        val drTxt = "$debitLabel:  $currency ${formatMoney(debit)}"
        body.text = "$crTxt\n$drTxt"
        body.setTextColor(Color.LTGRAY)
        body.textSize = 12f

        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        // center above the tapped point
        return MPPointF(-(width / 2f), -height.toFloat() - 12f)
    }
}

/* ---------- helpers ---------- */

private fun verticalFade(topColor: Int, bottomColor: Int): GradientDrawable =
    GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(
            Color.argb((0.28f * 255).toInt(), Color.red(topColor), Color.green(topColor), Color.blue(topColor)),
            Color.argb((0.00f * 255).toInt(), Color.red(bottomColor), Color.green(bottomColor), Color.blue(bottomColor))
        )
    ).apply { cornerRadius = 0f }

private fun formatMoney(v: Double): String {
    val s = if (kotlin.math.abs(v) >= 1000) "%,.2f".format(v) else "%.2f".format(v)
    return s.replace(',', ' ') // thin space
}