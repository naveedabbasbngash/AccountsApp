package com.mehfooz.accounts.app.ui

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
import com.google.firebase.auth.FirebaseAuth
import com.mehfooz.accounts.app.data.AppDatabase
import com.mehfooz.accounts.app.data.BootstrapManager
import com.mehfooz.accounts.app.data.DailyDebitsCredits
import com.mehfooz.accounts.app.ui.MonthTab
import com.mehfooz.accounts.app.net.Downloader
import kotlinx.coroutines.launch
import java.io.File
import java.net.URLEncoder

@Composable
fun ProfileScreen(onLogout: () -> Unit) {
    val TAG = "Sync"
    val DBG = "GraphDebug"

    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val user = FirebaseAuth.getInstance().currentUser

    // UI state moved here
    var askConfirm by remember { mutableStateOf(false) }
    var showOtp by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf(0f) }
    var downloading by remember { mutableStateOf(false) }
    var debugMode by remember { mutableStateOf(false) }
    var txCount by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        txCount = AppDatabase.get(ctx).transactionsP().count()
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Profile", style = MaterialTheme.typography.headlineMedium)
            Text("Signed in as: ${user?.email ?: user?.uid ?: "Unknown"}")
            Text("Local rows: ${txCount ?: "…"}")

            // Local DB import (assets)
            OutlinedButton(
                enabled = !downloading,
                onClick = {
                    scope.launch {
                        try {
                            status = "Importing local DB…"
                            downloading = true
                            BootstrapManager.importFromAsset(ctx, "live_seed.sqlite")
                            val total = AppDatabase.get(ctx).transactionsP().count()
                            txCount = total
                            status = "Local import complete. Transactions: $total"
                        } catch (e: Exception) {
                            status = "Local import error: ${(e.message ?: "unknown").take(200)}"
                            Log.e(TAG, "Local import error", e)
                        } finally {
                            downloading = false
                        }
                    }
                }
            ) { Text("Load Local DB (assets)") }

            // Server sync
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = debugMode, onCheckedChange = { debugMode = it })
                Spacer(Modifier.width(8.dp))
                Text(if (debugMode) "Debug JSON Mode" else "Full Sync Mode")
            }
            Button(onClick = { showOtp = true }, enabled = !downloading) { Text("Sync from Server") }

            if (downloading) {
                LinearProgressIndicator(progress = { progress })
                Text("Progress: ${(progress * 100).toInt()}%")
            }
            status?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }

            // Debug helpers
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        scope.launch { logRawSamples(ctx, DBG) }
                    }
                ) { Text("Log Raw") }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { askConfirm = true }, enabled = !downloading) {
                Text("Logout")
            }
        }
    }

    // OTP dialog (same as before, scoped here)
    if (showOtp) {
        OtpDialog(
            onDismiss = { showOtp = false },
            onVerify = { otp ->
                showOtp = false
                val email = user?.email
                if (email.isNullOrBlank()) {
                    status = "No email on Google account. Please re‑sign in."
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

/* ---------- shared bits moved here ---------- */

@Composable
private fun OtpDialog(onDismiss: () -> Unit, onVerify: (otp: String) -> Unit) {
    var otp by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter OTP") },
        text = {
            OutlinedTextField(
                value = otp,
                onValueChange = { if (it.length <= 6) otp = it.filter(Char::isDigit) },
                label = { Text("6‑digit OTP") },
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
}