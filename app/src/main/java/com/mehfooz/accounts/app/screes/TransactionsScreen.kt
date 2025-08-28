package com.mehfooz.accounts.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mehfooz.accounts.app.viewmodels.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    modifier: Modifier = Modifier
) {
    val deepBlue = Color(0xFF0B1E3A)
    val vm: TransactionsViewModel = viewModel()

    val search            by vm.search.collectAsStateWithLifecycle()
    val filter            by vm.filter.collectAsStateWithLifecycle()
    val items             by vm.items.collectAsStateWithLifecycle()
    val dateRange         by vm.dateRange.collectAsStateWithLifecycle()
    val balanceByCurrency by vm.balanceByCurrency.collectAsStateWithLifecycle()
    val selectedCurrency  by vm.selectedCurrency.collectAsStateWithLifecycle()
    val currencies        by vm.currenciesForSearch.collectAsStateWithLifecycle()

    val screenHPad = 6.dp
    val screenVPad = 8.dp

    var showBalanceMode by remember { mutableStateOf(false) }
    val canShowBalanceChip = search.isNotBlank() && balanceByCurrency.isNotEmpty()

    // Selected row to show in details panel
    var selectedRow by remember { mutableStateOf<TxItemUi?>(null) }

    // Leave balance mode & clear currency if user clears search
    LaunchedEffect(search) {
        if (search.isBlank() && showBalanceMode) showBalanceMode = false
        if (search.isBlank()) vm.setSelectedCurrency(null)
    }

    // Date pickers state
    var openDayPicker by remember { mutableStateOf(false) }
    var openRangePicker by remember { mutableStateOf(false) }
    val dayState   = rememberDatePickerState()
    val rangeState = rememberDateRangePickerState()

    // Date helpers
    val prettyFmt = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
    fun Long.toIso(): String = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate().toString()
    fun String.pretty(): String = LocalDate.parse(this).format(prettyFmt)
    val dateChipLabel = remember(dateRange) {
        val (s, e) = dateRange
        when {
            s == null && e == null -> "Dates"
            s != null && e == null -> s.pretty()
            s != null && e != null -> "${s.pretty()} – ${e.pretty()}"
            else -> "Dates"
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
        Text("Transactions", style = MaterialTheme.typography.headlineSmall, color = Color.White)

        // --- Search + suggestions (stacked vertically so suggestions are BELOW the box) ---
        var searchField by remember { mutableStateOf(TextFieldValue(search)) }

        Column {
            FancySearchWithSuggestions(
                value = searchField,
                onValueChange = { tf ->
                    searchField = tf
                    vm.setSearch(tf.text)
                },
                suggestions = remember(searchField.text, items) {
                    val q = searchField.text.trim()
                    if (q.isEmpty()) emptyList()
                    else items.map { it.name }
                        .distinct()
                        .filter { it.contains(q, ignoreCase = true) }
                        .take(10)
                },
                onSuggestionClick = { picked ->
                    // single-click select, cursor at end
                    searchField = TextFieldValue(picked, selection = TextRange(picked.length))
                    vm.setSearch(picked)
                },
                hint = "Search name…"
            )
        }

        // --- Chips row ---
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

            if (search.isNotBlank() && currencies.isNotEmpty()) {
                item {
                    CurrencyChipWithDropdown(
                        label = selectedCurrency ?: "Currency",
                        options = currencies,
                        onSelect = { cur -> vm.setSelectedCurrency(if (cur == "All") null else cur) },
                        showClear = (selectedCurrency != null)
                    )
                }
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
            item {
                DateChipWithDropdown(
                    label = dateChipLabel,
                    onPickSingle = { openDayPicker = true },
                    onPickRange  = { openRangePicker = true },
                    onClearDates = { vm.setDateRange(null, null) },
                    showClear    = (dateRange.first != null || dateRange.second != null)
                )
            }
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

        // --- Content card + details panel overlay ---
        Box(Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(8.dp),
                tonalElevation = 1.dp,
                shadowElevation = 2.dp,
                color = Color(0xFFF7F9FC)
            ) {
                if (showBalanceMode) {
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
                    if (items.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No results", color = Color(0xFF6B7280))
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 0.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            items(items, key = { it.voucherNo }) { row ->
                                TxListRow(
                                    item = row,
                                    onClick = { selectedRow = row } // open details panel
                                )
                                Divider(color = Color(0x14000000))
                            }
                        }
                    }
                }
            }

            // Animated details bottom panel
            this@Column.AnimatedVisibility(
                visible = selectedRow != null,
                enter = fadeIn(tween(200)) + expandVertically(
                    expandFrom = Alignment.Bottom,
                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                ),
                exit = fadeOut(tween(150)) + shrinkVertically(
                    shrinkTowards = Alignment.Bottom,
                    animationSpec = tween(200, easing = FastOutSlowInEasing)
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(10.dp)
            ) {
                selectedRow?.let { row ->
                    DetailsPanel(
                        row = row,
                        onClose = { selectedRow = null }
                    )
                }
            }
        }
    }

    // --- Date pickers ---
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

/* ============================
   Search + Suggestions (stacked vertically)
   ============================ */
@Composable
private fun FancySearchWithSuggestions(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    hint: String
) {
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current

    var anchorSize by remember { mutableStateOf(IntSize.Zero) }
    var open by remember { mutableStateOf(false) }
    var suppressOpen by remember { mutableStateOf(false) } // block reopen right after pick

    // Decide visibility; don't reopen if we just picked
    LaunchedEffect(value.text, suggestions, suppressOpen) {
        if (suppressOpen) {
            open = false
        } else {
            open = value.text.isNotBlank() && suggestions.isNotEmpty()
        }
        // if text exactly matches a suggestion, keep it closed
        if (suggestions.any { it.equals(value.text, ignoreCase = true) }) {
            open = false
            suppressOpen = true
        }
    }

    Column { // suggestions render BELOW the field
        OutlinedTextField(
            value = value,
            onValueChange = {
                suppressOpen = false           // typing allows panel again
                onValueChange(it)
            },
            placeholder = { Text(hint) },
            singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (value.text.isNotBlank()) {
                    IconButton(onClick = {
                        suppressOpen = false
                        onValueChange(TextFieldValue(""))
                        // keep keyboard if you like; comment next line to keep it open
                        // focusManager.clearFocus()
                    }) { Icon(Icons.Outlined.Clear, contentDescription = "Clear") }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { anchorSize = it.size },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White.copy(alpha = 0.10f),
                focusedContainerColor   = Color.White.copy(alpha = 0.14f),
                unfocusedBorderColor    = Color.Transparent,
                focusedBorderColor      = Color.White.copy(alpha = 0.30f),
                cursorColor             = Color.White,
                focusedTextColor        = Color.White,
                unfocusedTextColor      = Color.White,
                focusedPlaceholderColor   = Color.White.copy(alpha = 0.55f),
                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.55f),
                focusedLeadingIconColor   = Color.White,
                unfocusedLeadingIconColor = Color.White,
                focusedTrailingIconColor  = Color.White,
                unfocusedTrailingIconColor= Color.White
            )
        )

        if (open) {
            val widthDp = with(density) { anchorSize.width.toDp() }
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + expandVertically(),
                exit  = fadeOut() + shrinkVertically(),
                modifier = Modifier
                    .width(widthDp)
                    .padding(top = 6.dp) // gap under the field
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 3.dp,
                    shadowElevation = 6.dp,
                    color = Color(0xFFFDFEFF)
                ) {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 280.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(suggestions, key = { it }) { name ->
                            SuggestionRow(
                                text = name,
                                query = value.text,
                                onClick = {
                                    // 1) write picked text with caret at end
                                    onValueChange(TextFieldValue(name, TextRange(name.length)))
                                    // 2) notify VM/parent
                                    onSuggestionClick(name)
                                    // 3) close and suppress reopening until user types
                                    open = false
                                    suppressOpen = true
                                    // keep focus so keyboard stays up
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    text: String,
    query: String,
    onClick: () -> Unit
) {
    val annotated = remember(text, query) {
        if (query.isBlank()) AnnotatedString(text) else {
            val idx = text.indexOf(query, ignoreCase = true)
            if (idx < 0) AnnotatedString(text) else {
                AnnotatedString.Builder().apply {
                    append(text.substring(0, idx))
                    pushStyle(SpanStyle(fontWeight = FontWeight.SemiBold))
                    append(text.substring(idx, idx + query.length))
                    pop()
                    append(text.substring(idx + query.length))
                }.toAnnotatedString()
            }
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(annotated, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color(0xFF0B1E3A))
    }
}

/* ============================
   Currency chip with dropdown
   ============================ */
@Composable
private fun CurrencyChipWithDropdown(
    label: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    showClear: Boolean
) {
    var open by remember { mutableStateOf(false) }

    Box {
        AssistChip(
            onClick = { open = true },
            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            shape = RoundedCornerShape(50),
            colors = AssistChipDefaults.assistChipColors(
                containerColor = Color.White.copy(alpha = 0.16f),
                labelColor = Color.White
            ),
            border = null
        )

        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            DropdownMenuItem(
                text = { Text("All") },
                onClick = { open = false; onSelect("All") }
            )
            options.forEach { cur ->
                DropdownMenuItem(
                    text = { Text(cur) },
                    onClick = { open = false; onSelect(cur) }
                )
            }
            if (showClear) {
                DropdownMenuItem(
                    text = { Text("Clear") },
                    onClick = { open = false; onSelect("All") }
                )
            }
        }
    }
}

/* ---------------- Date chip ---------------- */
@Composable
fun DateChipWithDropdown(
    label: String,
    onPickSingle: () -> Unit,
    onPickRange: () -> Unit,
    onClearDates: () -> Unit,
    showClear: Boolean
) {
    var open by remember { mutableStateOf(false) }

    Box {
        AssistChip(
            onClick = { open = true },
            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            shape = RoundedCornerShape(50),
            colors = AssistChipDefaults.assistChipColors(
                containerColor = Color.White.copy(alpha = 0.16f),
                labelColor = Color.White
            ),
            border = null
        )

        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            DropdownMenuItem(
                text = { Text("Pick single day") },
                onClick = { open = false; onPickSingle() }
            )
            DropdownMenuItem(
                text = { Text("Pick date range") },
                onClick = { open = false; onPickRange() }
            )
            if (showClear) {
                DropdownMenuItem(
                    text = { Text("Clear dates") },
                    onClick = { open = false; onClearDates() }
                )
            }
        }
    }
}

/* ---------------- Chips ---------------- */
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

/* ---------------- Transactions Row (clickable) ---------------- */
@Composable
private fun TxListRow(
    item: TxItemUi,
    onClick: () -> Unit
) {
    val isCredit = item.crUnits > 0f
    val amountColor = if (isCredit) Color(0xFF2E7D32) else Color(0xFFC62828)
    val sign = if (isCredit) "+" else "−"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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

/* ---------------- Details Bottom Panel ---------------- */
@Composable
private fun DetailsPanel(
    row: TxItemUi,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Voucher #${row.voucherNo}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF0B1E3A),
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onClose) { Text("Close") }
            }
            Spacer(Modifier.height(4.dp))
            Divider()
            Spacer(Modifier.height(8.dp))

            InfoLine("Date", row.date)
            InfoLine("Name", row.name.ifBlank { "Unknown" })
            InfoLine("Currency", row.currency)
            if (row.description.isNotBlank()) {
                InfoLine("Description", row.description)
            }

            val isCredit = row.crUnits > 0f
            val amount = if (isCredit) row.crUnits else row.drUnits
            val sign = if (isCredit) "+" else "−"
            val color = if (isCredit) Color(0xFF2E7D32) else Color(0xFFC62828)
            Spacer(Modifier.height(8.dp))
            Text("Amount", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B7280))
            Text(
                "$sign ${money(amount)}",
                style = MaterialTheme.typography.headlineSmall,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B7280))
        Text(value, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF0B1E3A))
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
    if (kotlin.math.abs(v) >= 1000f) "%,.2f".format(v) else "%.2f".format(v)@Composable
private fun BalanceList(
    name: String,
    rows: List<BalanceCurrencyUi>
) {
    Column(Modifier.fillMaxSize()) {
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

@Composable
private fun BalanceCurrencyRow(b: BalanceCurrencyUi) {
    val green = Color(0xFF2E7D32)
    val red   = Color(0xFFC62828)
    val balColor = if (b.balanceUnits >= 0f) green else red

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left column: labels + amounts
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp, end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                b.currency.ifBlank { "Unknown" },
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF0B1E3A)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Credit", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
                Text(
                    money(b.creditUnits),
                    style = MaterialTheme.typography.bodyLarge,
                    color = green,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                Text("Debit", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
                Text(
                    money(b.debitUnits),
                    style = MaterialTheme.typography.bodyLarge,
                    color = red,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Right column: mini pie + balance
        Column(
            modifier = Modifier.padding(start = 8.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Uses your existing MiniCreditDebitPie composable
            MiniCreditDebitPie(
                credit = b.creditUnits,
                debit  = b.debitUnits,
                size   = 108.dp,
                green  = 0xFF2E7D32.toInt(),
                red    = 0xFFC62828.toInt()
            )
            Spacer(Modifier.height(8.dp))
            Text("Balance", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
            Text(
                money(b.balanceUnits),
                color = balColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}