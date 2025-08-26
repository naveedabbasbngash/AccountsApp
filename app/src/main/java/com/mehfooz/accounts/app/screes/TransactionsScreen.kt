package com.mehfooz.accounts.app.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mehfooz.accounts.app.viewmodels.TransactionsViewModel
import com.mehfooz.accounts.app.viewmodels.TxFilter
import com.mehfooz.accounts.app.viewmodels.TxItemUi
import com.mehfooz.accounts.app.viewmodels.BalanceCurrencyUi
import kotlin.math.abs
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import android.graphics.Color as AColor
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    modifier: Modifier = Modifier
) {
    val deepBlue = Color(0xFF0B1E3A)
    val vm: TransactionsViewModel = viewModel()

    val search           by vm.search.collectAsStateWithLifecycle()
    val filter           by vm.filter.collectAsStateWithLifecycle()
    val items            by vm.items.collectAsStateWithLifecycle()
    val dateRange        by vm.dateRange.collectAsStateWithLifecycle()         // Pair<String?, String?>
    val balanceByCurrency by vm.balanceByCurrency.collectAsStateWithLifecycle() // List<BalanceCurrencyUi>

    // 🔧 spacing knobs
    val screenHPad = 6.dp
    val screenVPad = 8.dp

    // Local UI toggle: when true, show Balance (per-currency) view instead of transactions list
    var showBalanceMode by remember { mutableStateOf(false) }

    // Only show the Balance chip if there's a search name AND we have some balance rows
    val canShowBalanceChip = search.isNotBlank() && balanceByCurrency.isNotEmpty()

    // If user clears search, automatically leave balance mode
    LaunchedEffect(search) {
        if (search.isBlank() && showBalanceMode) showBalanceMode = false
    }

    // Date pickers state
    var openDateMenu    by remember { mutableStateOf(false) }
    var openDayPicker   by remember { mutableStateOf(false) }
    var openRangePicker by remember { mutableStateOf(false) }
    val dayState   = rememberDatePickerState()
    val rangeState = rememberDateRangePickerState()

    val prettyFmt = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
    fun Long.toIso(): String = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate().toString()
    fun String.pretty(): String = LocalDate.parse(this).format(prettyFmt)

    val dateChipLabel = remember(dateRange) {
        val (s, e) = dateRange
        when {
            s == null && e == null -> "All dates"
            s != null && e == null -> s.pretty()
            s != null && e != null -> "${s.pretty()} – ${e.pretty()}"
            else -> "All dates"
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .background(deepBlue)
            .statusBarsPadding()
            .padding(horizontal = screenHPad, vertical = screenVPad),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Title
        Text("Transactions", style = MaterialTheme.typography.headlineSmall, color = Color.White)

        // Search
        OutlinedTextField(
            value = search,
            onValueChange = {
                vm.setSearch(it)
                // Leaving Balance mode if user edits the name (optional UX)
                if (showBalanceMode) showBalanceMode = false
            },
            placeholder = { Text("Search name…") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                focusedContainerColor   = Color.White.copy(alpha = 0.12f),
                unfocusedBorderColor    = Color.Transparent,
                focusedBorderColor      = Color.White.copy(alpha = 0.30f),
                cursorColor             = Color.White,
                focusedTextColor        = Color.White,
                unfocusedTextColor      = Color.White,
                focusedPlaceholderColor   = Color.White.copy(alpha = 0.55f),
                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.55f),
                focusedLeadingIconColor   = Color.White,
                unfocusedLeadingIconColor = Color.White
            )
        )

        // Scrollable chips row (All / Debits / Credits / Date / Balance)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            item {
                FilterChip_(
                    selected = !showBalanceMode && filter == TxFilter.ALL,
                    onClick = { showBalanceMode = false; vm.setFilter(TxFilter.ALL) },
                    label = "All"
                )
            }
            item {
                FilterChip_(
                    selected = !showBalanceMode && filter == TxFilter.DEBIT,
                    onClick = { showBalanceMode = false; vm.setFilter(TxFilter.DEBIT) },
                    label = "Debits"
                )
            }
            item {
                FilterChip_(
                    selected = !showBalanceMode && filter == TxFilter.CREDIT,
                    onClick = { showBalanceMode = false; vm.setFilter(TxFilter.CREDIT) },
                    label = "Credits"
                )
            }
            // Date chip
            item {
                AssistChip(
                    onClick = { openDateMenu = true },
                    label = { Text(dateChipLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = { Icon(Icons.Outlined.DateRange, contentDescription = null) },
                    shape = RoundedCornerShape(50),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color.White.copy(alpha = 0.16f),
                        labelColor = Color.White
                    ),
                    border = null
                )
            }
            // Balance chip (shown only when a name is typed AND data exists)
            if (canShowBalanceChip) {
                item {
                    FilterChip_(
                        selected = showBalanceMode,
                        onClick = { showBalanceMode = true },
                        label = "Balance"
                    )
                }
            }
        }

        // Date picker dropdown
        DropdownMenu(expanded = openDateMenu, onDismissRequest = { openDateMenu = false }) {
            DropdownMenuItem(
                text = { Text("Pick single day") },
                onClick = { openDateMenu = false; openDayPicker = true }
            )
            DropdownMenuItem(
                text = { Text("Pick date range") },
                onClick = { openDateMenu = false; openRangePicker = true }
            )
            if (dateRange.first != null || dateRange.second != null) {
                DropdownMenuItem(
                    text = { Text("Clear dates") },
                    onClick = { openDateMenu = false; vm.setDateRange(null, null) }
                )
            }
        }

        // Content card
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 1.dp,
            shadowElevation = 2.dp,
            color = Color(0xFFF7F9FC)
        ) {
            if (showBalanceMode) {
                // ========== BALANCE (PER-CURRENCY) VIEW ==========
                if (balanceByCurrency.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No balance data", color = Color(0xFF6B7280))
                    }
                } else {
                    BalanceList(
                        name = search,
                        rows = balanceByCurrency
                    )
                }
            } else {
                // ========== NORMAL TRANSACTIONS LIST ==========
                if (items.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No results", color = Color(0xFF6B7280))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 0.dp, bottom = 0.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(items, key = { it.voucherNo }) { row ->
                            TxListRow(row)
                            Divider(color = Color(0x14000000))
                        }
                    }
                }
            }
        }
    }

    /* ------------------ Date pickers ------------------ */

    if (openDayPicker) {
        DatePickerDialog(
            onDismissRequest = { openDayPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dayState.selectedDateMillis?.let { vm.setDateRange(it.toIso(), it.toIso()) }
                        openDayPicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { openDayPicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = dayState) }
    }

    if (openRangePicker) {
        DatePickerDialog(
            onDismissRequest = { openRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val s = rangeState.selectedStartDateMillis
                        val e = rangeState.selectedEndDateMillis
                        if (s != null && e != null) vm.setDateRange(s.toIso(), e.toIso())
                        openRangePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { openRangePicker = false }) { Text("Cancel") } }
        ) { DateRangePicker(state = rangeState) }
    }
}

/* ---------------------------------------------------------
   BALANCE LIST (separate, easy to restyle later)
   --------------------------------------------------------- */
/* ============================
   BALANCE LIST (header + rows)
   ============================ */

@Composable
private fun BalanceList(
    name: String,
    rows: List<BalanceCurrencyUi>
) {
    Column(Modifier.fillMaxSize()) {
        // Optional header for the searched person
        if (name.isNotBlank()) {
            Text(
                text = "Balance • $name",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF0B1E3A)
            )
            Divider(color = Color(0x14000000))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(0.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(rows, key = { it.currency }) { row ->
                BalanceCurrencyRow(row)
                Divider(color = Color(0x14000000))
            }
        }
    }
}

/* ===========================================
   ONE CURRENCY ROW (stacked + pie on the right)
   - Left: Currency title, Credit (top), Debit (bottom)
   - Right: Mini pie; Balance centered below the pie
   =========================================== */

@Composable
private fun BalanceCurrencyRow(b: BalanceCurrencyUi) {
    val green = Color(0xFF2E7D32)
    val red   = Color(0xFFC62828)
    val balColor = if (b.balanceUnits >= 0f) green else red

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp), // overall row gutter
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LEFT: push a *little* to the right (more start padding)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp, end = 8.dp), // ← nudge right column inwards
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Currency label (optional—remove if you don’t want it here)
            Text(
                b.currency.ifBlank { "Unknown" },
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF0B1E3A)
            )

            // Credit (top) + Debit (below)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Credit",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280)
                )
                Text(
                    money(b.creditUnits),
                    style = MaterialTheme.typography.bodyLarge,
                    color = green,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    "Debit",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280)
                )
                Text(
                    money(b.debitUnits),
                    style = MaterialTheme.typography.bodyLarge,
                    color = red,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // RIGHT: pie + balance, nudged a *little* to the left (more end padding)
        Column(
            modifier = Modifier
                .padding(start = 8.dp, end = 16.dp), // ← nudge toward center
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pie reflects actual values: green = credit, red = debit
            MiniCreditDebitPie(
                credit = b.creditUnits,
                debit  = b.debitUnits,
                size   = 108.dp,                           // 🔧 change size if needed
                green  = 0xFF2E7D32.toInt(),               // credit color
                red    = 0xFFC62828.toInt()                // debit color
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Balance",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B7280)
            )
            Text(
                text = money(b.balanceUnits),
                color = balColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}/* ---------------- Chips ---------------- */

@Composable
private fun FilterChip_(
    selected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        shape = RoundedCornerShape(50),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.10f),
            labelColor = Color.White
        ),
        border = null
    )
}

/* ---------------- Transactions Row ---------------- */

@Composable
private fun TxListRow(item: TxItemUi) {
    val isCredit = item.crUnits > 0f
    val amountColor = if (isCredit) Color(0xFF2E7D32) else Color(0xFFC62828)
    val sign = if (isCredit) "+" else "−"

    // Debug log once per row appearance
    LaunchedEffect(item) {
        Log.d(
            "TxListRow",
            """
            ---- Transaction Row ----
            VoucherNo   = ${item.voucherNo}
            Date        = ${item.date}
            Name        = ${item.name}
            Desc        = ${item.description}
            Credit?     = $isCredit
            DebitUnits  = ${item.drUnits}
            CreditUnits = ${item.crUnits}
            Amount      = ${item.amountUnits}
            Currency    = ${item.currency}
            -------------------------
            """.trimIndent()
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar (first letter)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(avatarColorFor(item.name)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                item.name.firstOrNull()?.uppercase() ?: "?",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.width(12.dp))

        // LEFT column
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(item.date, style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B7280))
            Text(item.name.ifBlank { "Unknown" }, style = MaterialTheme.typography.titleSmall, color = Color(0xFF0B1E3A))
            Text(
                item.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B7280),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(12.dp))

        // RIGHT: amount + currency (stacked)
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "$sign ${money(item.amountUnits)}",
                color = amountColor,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(item.currency, style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B7280))
        }
    }
}

/* ---------------- Utils ---------------- */

private fun avatarColorFor(name: String): Color {
    if (name.isBlank()) return Color(0xFF6C5CE7)
    val palette = listOf(
        0xFF6C5CE7, 0xFF00B894, 0xFF0984E3, 0xFFFF7675,
        0xFFFD79A8, 0xFF00CEC9, 0xFF74B9FF, 0xFFA29BFE
    ).map { Color(it) }
    val idx = abs(name.lowercase().hashCode()) % palette.size
    return palette[idx]
}

private fun money(v: Float): String =
    if (kotlin.math.abs(v) >= 1000f) "%,.2f".format(v) else "%.2f".format(v)