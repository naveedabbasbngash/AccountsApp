package com.mehfooz.accounts.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.mehfooz.accounts.app.model.DayValue

@Composable
fun OverviewScreen() {
    // This color will visually become your status bar color because we draw behind it.
    val deepBlue = Color(0xFF0B1E3A)

    val vm: DashboardViewModel = viewModel()
    val tab by vm.tab.collectAsStateWithLifecycle()
    val dailyDC by vm.dailyDC.collectAsStateWithLifecycle()

    val credits = remember(dailyDC) { dailyDC.map { DayValue(it.day, it.crUnits) } }
    val debits  = remember(dailyDC) { dailyDC.map { DayValue(it.day, it.drUnits) } }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = deepBlue    // paint the whole root deep blue (shows behind status bar)
    ) {
        Column(Modifier.fillMaxSize()) {

            // ==== HERO (edge-to-edge, chart touches sides) ====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(deepBlue) // keep hero area blue too
            ) {
                // Top row: title + month chips; keep them below the status bar icons
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()                // <-- pushes content below status bar
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Overview",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium
                    )

                    MonthChipsRow(
                        selected = tab,
                        onSelect = { vm.setTab(it) }
                    )
                }

                // Chart area below the header, edge-to-edge horizontally
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp) // adjust if you want more/less hero height
                        .align(Alignment.BottomCenter)
                ) {
                    FinanceNeoCard(
                        selectedTab = tab,
                        onTabChange = { vm.setTab(it) },
                        credits = credits,
                        debits  = debits,
                        currency = "" // "Rs", "$", etc. if you prefer
                    )
                }
            }

            // ==== BODY (white background content below) ====
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)               // body is white
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Place totals / lists / whatever you want here.
                // Example:
                // val totalCredits = dailyDC.sumOf { it.crUnits.toDouble() }
                // val totalDebits  = dailyDC.sumOf { it.drUnits.toDouble() }
                // Text("Credits: ${"%.2f".format(totalCredits)}", color = deepBlue)
                // Text("Debits : ${"%.2f".format(totalDebits)}",  color = deepBlue)

                // If your body has content near the bottom and you want to avoid
                // overlapping the nav bar (also transparent in edge-to-edge),
                // add .navigationBarsPadding() to the last element or this Column.
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}
/** Small chip row used on the hero header */
@Composable
private fun MonthChipsRow(
    selected: MonthTab,
    onSelect: (MonthTab) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        androidx.compose.material3.FilterChip(
            selected = selected == MonthTab.THIS_MONTH,
            onClick = { onSelect(MonthTab.THIS_MONTH) },
            label = { androidx.compose.material3.Text("This month") }
        )
        androidx.compose.material3.FilterChip(
            selected = selected == MonthTab.LAST_MONTH,
            onClick = { onSelect(MonthTab.LAST_MONTH) },
            label = { androidx.compose.material3.Text("Last month") }
        )
    }
}