package com.mehfooz.accounts.app.ui

import android.graphics.Color
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as JColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.mehfooz.accounts.app.model.DayValue
import kotlin.math.abs

/* =========================================================
   OVERVIEW SCREEN
   ========================================================= */
@Composable
fun OverviewScreen() {
    val deepBlue = JColor(0xFF0B1E3A)
    val vm: DashboardViewModel = viewModel()

    val tab by vm.tab.collectAsStateWithLifecycle()
    val dailyDC by vm.dailyDC.collectAsStateWithLifecycle()
    val piesByCur by vm.pieByCurrency.collectAsStateWithLifecycle()

    val credits = remember(dailyDC) { dailyDC.map { DayValue(it.day, it.crUnits) } }
    val debits  = remember(dailyDC) { dailyDC.map { DayValue(it.day, it.drUnits) } }

    Surface(modifier = Modifier.fillMaxSize(), color = deepBlue) {
        Column(Modifier.fillMaxSize()) {

            // ===== HERO (blue) =====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(deepBlue)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Overview", color = JColor.White, style = MaterialTheme.typography.headlineMedium)
                    MonthChipsRow(selected = tab, onSelect = { vm.setTab(it) })
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    FinanceNeoCard(
                        selectedTab = tab,
                        onTabChange = { vm.setTab(it) },
                        credits = credits,
                        debits = debits,
                        currency = "" // or "Rs", "$"
                    )
                }
            }

            // ===== BODY (scrollable) =====
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 7.dp, vertical = 10.dp), // side/top padding only
                verticalArrangement = Arrangement.spacedBy(1.dp),
                contentPadding = PaddingValues(bottom = -0.dp)      // <— no fixed bottom gap
            ) {
                item {
                    CurrencyBreakdownCard(
                        title = "By Currency",
                        rows = piesByCur,
                        cornerRadius = 12.dp // 🔧 change this to adjust the card's corner radius
                    )
                }
            }
        }
    }
}
/* ---------------- Header chips ---------------- */
@Composable
private fun MonthChipsRow(
    selected: MonthTab,
    onSelect: (MonthTab) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        FilterChip(
            selected = selected == MonthTab.THIS_MONTH,
            onClick = { onSelect(MonthTab.THIS_MONTH) },
            label = { Text("This month") }
        )
        FilterChip(
            selected = selected == MonthTab.LAST_MONTH,
            onClick = { onSelect(MonthTab.LAST_MONTH) },
            label = { Text("Last month") }
        )
    }
}

/* =========================================================
   ONE CARD WITH ALL CURRENCIES (rows)
   Left (stacked): Credit (top) + Debit (bottom)
   Right: mini pie
   Card background: light to contrast the deep blue screen
   ========================================================= */

@Composable
fun CurrencyBreakdownCard(
    title: String,
    rows: List<DashboardViewModel.CurrencyPieBucket>,
    cornerRadius: Dp = 24.dp // 🔧 change this to adjust the card's radius
) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius)

    // Soft light container (not blue) so it pops on the deep-blue screen
    val container = JColor(0xFFF7F9FC)
    val green = JColor(0xFF2E7D32)
    val red = JColor(0xFFC62828)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(container)
            /* 🔧 Inner padding of the card. Increase/decrease to change space
               between card border and its contents. */
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(title, color = JColor(0xFF0B1E3A), style = MaterialTheme.typography.titleMedium)

        rows.forEachIndexed { idx, bucket ->
            CurrencyRow(
                name = bucket.currency.ifBlank { "Unknown" },
                symbol = bucket.currencySymbol,
                slices = bucket.slices,
                green = green,
                red = red
            )

            if (idx < rows.lastIndex) {
                Divider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = 0.5.dp,
                    color = JColor.Black.copy(alpha = 0.08f)
                )
            }
        }
    }
}

/* ---------------- A single currency row ---------------- */
@Composable
private fun CurrencyRow(
    name: String,
    symbol: String,
    slices: List<DashboardViewModel.PieSlice>, // expect 2 slices: ["Credit", v], ["Debit", v]
    green: JColor,
    red: JColor
) {
    // Pull values from slices (case-insensitive match)
    val cr = remember(slices) { slices.firstOrNull { it.label.equals("credit", true) }?.value ?: 0f }
    val dr = remember(slices) { slices.firstOrNull { it.label.equals("debit",  true) }?.value ?: 0f }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LEFT: title + Credit (top) + Debit (bottom)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 10.dp), // 🔧 gap between text column and mini pie
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(name, color = JColor(0xFF0B1E3A), style = MaterialTheme.typography.titleSmall)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Credit on top
                Column(horizontalAlignment = Alignment.Start) {
                    Text("Credit - Jama", color = JColor(0xFF6B7280), style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "${if (symbol.isNotBlank()) "$symbol " else ""}${money(cr)}",
                        color = green,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                // Debit below
                Column(horizontalAlignment = Alignment.Start) {
                    Text("Debit - Banam", color = JColor(0xFF6B7280), style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "${if (symbol.isNotBlank()) "$symbol " else ""}${money(dr)}",
                        color = red,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        // RIGHT: small pie (kept very light for smoothness)
        MiniCreditDebitPie(
            credit = cr,
            debit = dr,
            size = 100.dp, // 🔧 change this to make the mini pie bigger/smaller
            green = android.graphics.Color.parseColor("#2E7D32"),
            red = android.graphics.Color.parseColor("#C62828")
        )
    }
}

/* ---------------- tiny (safe) MPAndroidChart pie ---------------- */
@Composable
fun MiniCreditDebitPie(
    credit: Float,
    debit: Float,
    size: Dp,
    green: Int,
    red: Int
) {
    val total = remember(credit, debit) { (credit + debit).coerceAtLeast(0f) }
    val entries = remember(credit, debit) {
        buildList {
            if (credit > 0f) add(PieEntry(credit, "Credit"))
            if (debit  > 0f) add(PieEntry(debit,  "Debit"))
        }.ifEmpty { listOf(PieEntry(1f, "")) }
    }

    AndroidView(
        modifier = Modifier.size(size),
        factory = { ctx ->
            com.github.mikephil.charting.charts.PieChart(ctx).apply {
                description.isEnabled = false
                isRotationEnabled = false
                legend.isEnabled = false
                setDrawEntryLabels(false)
                setUsePercentValues(false)
                setMinAngleForSlices(0f)
                setTouchEnabled(false) // smoother scroll

                // ✅ SOLID PIE — no hole
                isDrawHoleEnabled = false
                holeRadius = 0f
                transparentCircleRadius = 0f

                setExtraOffsets(0f, 0f, 0f, 0f)
                setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)

                data = placeholderPieData()
                visibility = android.view.View.INVISIBLE
            }
        },
        update = { chart ->
            if (total <= 0f) {
                chart.data = placeholderPieData()
                chart.visibility = android.view.View.INVISIBLE
                chart.invalidate()
                return@AndroidView
            }

            val set = com.github.mikephil.charting.data.PieDataSet(entries, "").apply {
                // green = credit, red = debit
                colors = when (entries.size) {
                    2 -> listOf(green, red)
                    1 -> listOf(if (credit > 0f) green else red)
                    else -> listOf(green, red)
                }
                sliceSpace = 0f // 🔧 set to 1–2f if you want tiny gaps
                valueTextColor = android.graphics.Color.TRANSPARENT
                valueTextSize = 0f
                valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                    override fun getFormattedValue(value: Float) = ""
                }
            }

            chart.data = com.github.mikephil.charting.data.PieData(set)
            chart.highlightValues(null)
            chart.visibility = android.view.View.VISIBLE
            chart.invalidate()
        }
    )
}

/* ---------------- helpers ---------------- */

private fun placeholderPieData(): PieData {
    val set = PieDataSet(listOf(PieEntry(1f, "")), "").apply {
        colors = listOf(Color.TRANSPARENT)
        valueTextColor = Color.TRANSPARENT
        valueTextSize = 0f
    }
    return PieData(set)
}

private fun money(v: Float): String =
    if (abs(v) >= 1000f) "%,.2f".format(v) else "%.2f".format(v)