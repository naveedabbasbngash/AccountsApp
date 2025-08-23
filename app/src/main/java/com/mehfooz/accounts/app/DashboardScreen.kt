package com.mehfooz.accounts.app

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.mehfooz.accounts.app.data.AppDatabase
import com.mehfooz.accounts.app.data.BootstrapManager
import com.mehfooz.accounts.app.data.DailyDebitsCredits
import com.mehfooz.accounts.app.net.Downloader
import com.mehfooz.accounts.app.ui.DashboardViewModel
import com.mehfooz.accounts.app.ui.FinanceChartCardMp
import com.mehfooz.accounts.app.ui.MonthTab
import com.mehfooz.accounts.app.model.DayValue
import com.mehfooz.accounts.app.ui.FinanceNeoCard
import kotlinx.coroutines.launch
import java.io.File
import java.net.URLEncoder

@Composable
fun DashboardScreen(onLogout: () -> Unit) {
    val TAG = "Sync"
    val DBG = "GraphDebug"

    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    // Firebase user
    val user = FirebaseAuth.getInstance().currentUser

    // UI states
    var askConfirm by remember { mutableStateOf(false) }
    var showOtp by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf(0f) }
    var downloading by remember { mutableStateOf(false) }
    var debugMode by remember { mutableStateOf(false) }

    // --- ViewModel ---
    val vm: DashboardViewModel = viewModel()
    val tab by vm.tab.collectAsStateWithLifecycle()
    val dailyDC by vm.dailyDC.collectAsStateWithLifecycle()

    // Map Room results -> MPChart inputs (already in UNITS from DAO)
    val credits: List<DayValue> = remember(dailyDC) { dailyDC.map { DayValue(it.day, it.crUnits) } }
    val debits : List<DayValue> = remember(dailyDC) { dailyDC.map { DayValue(it.day, it.drUnits) } }

    // Totals (in units)
    val totalCredits = remember(dailyDC) { dailyDC.sumOf { it.crUnits.toDouble() } }
    val totalDebits  = remember(dailyDC) { dailyDC.sumOf { it.drUnits.toDouble() } }
    val monthNet     = totalCredits - totalDebits

    // Local row counter
    var txCount by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(Unit) {
        txCount = AppDatabase.get(ctx).transactionsP().count()
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Dashboard", style = MaterialTheme.typography.headlineMedium)
            Text("Signed in as: ${user?.email ?: user?.uid ?: "Unknown"}")

            // Tabs
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = tab == MonthTab.THIS_MONTH,
                    onClick = { vm.setTab(MonthTab.THIS_MONTH) },
                    label = { Text("This month") }
                )
                FilterChip(
                    selected = tab == MonthTab.LAST_MONTH,
                    onClick = { vm.setTab(MonthTab.LAST_MONTH) },
                    label = { Text("Last month") }
                )
            }

            // Debug: see total units for each series
            println("credit total = ${credits.sumOf { it.amountUnits.toDouble() }}")
            println("debit total  = ${debits.sumOf { it.amountUnits.toDouble() }}")

            // ---- Graph card: two smooth lines (credits vs debits) ----
            FinanceNeoCard(
                selectedTab = tab,
                onTabChange = { vm.setTab(it) },
                credits = credits,
                debits  = debits,
                currency = "" // or "Rs", "$", etc.
            )

            Spacer(Modifier.height(12.dp))
            Text("Total Credits: ${"%.2f".format(totalCredits)}")
            Text("Total Debits:  ${"%.2f".format(totalDebits)}")
            Text("Net:           ${"%.2f".format(monthNet)}")
            Text("Local rows: ${txCount ?: "…"}")

            // --- Debug buttons ---
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    scope.launch { logMonthSnapshot(dailyDC, DBG, tab) }
                }) { Text("Log Snapshot") }

                OutlinedButton(onClick = {
                    scope.launch { logRawSamples(ctx, DBG) }
                }) { Text("Log Raw") }

                // NEW: import DB from assets (e.g. app/src/main/assets/live_seed.sqlite)
                OutlinedButton(
                    enabled = !downloading,
                    onClick = {
                        scope.launch {
                            try {
                                status = "Importing local DB…"
                                downloading = true
                                // <<< change ONLY the filename if yours is different >>>
                                BootstrapManager.importFromAsset(ctx, "live_seed.sqlite")
                                // Touch Room so it reopens and we refresh counts
                                val total = AppDatabase.get(ctx).transactionsP().count()
                                txCount = total
                                status = "Local import complete. Transactions: $total"
                                // Optional: log for the active tab
                                logMonthSnapshot(dailyDC = vm.dailyDC.value, TAG = DBG, tab = vm.tab.value)
                            } catch (e: Exception) {
                                status = "Local import error: ${(e.message ?: "unknown").take(200)}"
                                Log.e(TAG, "Local import error", e)
                            } finally {
                                downloading = false
                            }
                        }
                    }
                ) { Text("Load Local DB") }
            }

            // --- Sync (server) ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = debugMode, onCheckedChange = { debugMode = it })
                Spacer(Modifier.width(8.dp))
                Text(if (debugMode) "Debug JSON Mode" else "Full Sync Mode")
            }

            Button(onClick = { showOtp = true }, enabled = !downloading) { Text("Sync") }

            if (downloading) {
                LinearProgressIndicator(progress = { progress })
                Text("Progress: ${(progress * 100).toInt()}%")
            }

            status?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }

            OutlinedButton(onClick = { askConfirm = true }, enabled = !downloading) {
                Text("Logout")
            }
        }
    }

    // OTP Dialog
    if (showOtp) {
        OtpDialog(
            onDismiss = { showOtp = false },
            onVerify = { otp ->
                showOtp = false
                val email = user?.email
                if (email.isNullOrBlank()) {
                    status = "No email on Google account. Please re-sign in."
                    return@OtpDialog
                }
                scope.launch {
                    try {
                        downloading = true
                        progress = 0f
                        val base = "http://kheloaurjeeto.net/Apps/pairing-api/sync/download.php"
                        val qp = "email=${URLEncoder.encode(email, "UTF-8")}&otp=${URLEncoder.encode(otp, "UTF-8")}" +
                                if (debugMode) "&debug=1" else ""
                        val fullUrl = "$base?$qp"
                        Log.d(TAG, "Request: $fullUrl")

                        if (debugMode) {
                            status = "Debug check…"
                            val dest = File(ctx.filesDir, "server_debug.json")
                            Downloader.downloadWithProgress(
                                baseUrl = base,
                                queryParams = mapOf("email" to email, "otp" to otp, "debug" to "1"),
                                destFile = dest
                            ) { p -> progress = p }
                            status = "Debug JSON saved: ${dest.absolutePath}"
                        } else {
                            status = "Downloading full DB…"
                            BootstrapManager.bootstrapFromFull(ctx, fullUrl) { pct -> progress = pct / 100f }
                            val total = AppDatabase.get(ctx).transactionsP().count()
                            txCount = total
                            status = "Sync complete. Transactions: $total"
                            logMonthSnapshot(dailyDC = vm.dailyDC.value, TAG = DBG, tab = vm.tab.value)
                        }
                    } catch (e: Exception) {
                        status = "Error: ${(e.message ?: "unknown").take(4000)}"
                        Log.e(TAG, "Sync error", e)
                    } finally {
                        downloading = false
                    }
                }
            }
        )
    }

    // Logout confirm
    if (askConfirm) {
        AlertDialog(
            onDismissRequest = { askConfirm = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(onClick = { askConfirm = false; onLogout() }) { Text("Yes, sign out") }
            },
            dismissButton = {
                TextButton(onClick = { askConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

/* -------------------- OTP -------------------- */
@Composable
private fun OtpDialog(onDismiss: () -> Unit, onVerify: (otp: String) -> Unit) {
    var otp by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter OTP") },
        text = {
            OutlinedTextField(
                value = otp,
                onValueChange = { if (it.length <= 6) otp = it.filter { ch -> ch.isDigit() } },
                label = { Text("6-digit OTP") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(enabled = otp.length == 6, onClick = { onVerify(otp) }) { Text("Verify") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/* -------------------- Debug helpers -------------------- */
private fun logMonthSnapshot(
    dailyDC: List<DailyDebitsCredits>,
    TAG: String,
    tab: MonthTab
) {
    val cr = dailyDC.sumOf { it.crUnits.toDouble() }
    val dr = dailyDC.sumOf { it.drUnits.toDouble() }
    Log.d(TAG, "=== Month Snapshot ($tab) ===")
    Log.d(TAG, "Days with data: ${dailyDC.size}, Credits=${"%.2f".format(cr)}, Debits=${"%.2f".format(dr)}, Net=${"%.2f".format(cr - dr)}")
    dailyDC.take(40).forEach { d ->
        Log.d(TAG, "  day=${d.day}  cr=${"%.2f".format(d.crUnits)}  dr=${"%.2f".format(d.drUnits)}")
    }
}

private suspend fun logRawSamples(ctx: android.content.Context, TAG: String) {
    val adb = AppDatabase.get(ctx)
    val sql = adb.openHelper.readableDatabase

    sql.query("SELECT COUNT(*) c, MIN(TDate) minD, MAX(TDate) maxD FROM Transactions_P").use {
        if (it.moveToFirst()) {
            Log.d(TAG, "Rows=${it.getLong(0)}  TDate MIN=${it.getString(1)}  MAX=${it.getString(2)}")
        }
    }

    sql.query(
        """
        SELECT substr(TDate,1,7) ym, COUNT(*) cnt
        FROM Transactions_P
        GROUP BY ym ORDER BY ym DESC LIMIT 12
        """.trimIndent()
    ).use {
        Log.d(TAG, "Recent year-month buckets:")
        while (it.moveToNext()) Log.d(TAG, "  ${it.getString(0)} -> ${it.getLong(1)} rows")
    }

    sql.query(
        """
        SELECT TDate,VoucherNo,DrCents,CrCents
        FROM Transactions_P ORDER BY TDate DESC,VoucherNo DESC LIMIT 20
        """.trimIndent()
    ).use {
        Log.d(TAG, "Top 20 rows:")
        while (it.moveToNext()) {
            Log.d(TAG, "  ${it.getString(0)}  VNo=${it.getLong(1)} Dr=${it.getLong(2)} Cr=${it.getLong(3)}")
        }
    }
}