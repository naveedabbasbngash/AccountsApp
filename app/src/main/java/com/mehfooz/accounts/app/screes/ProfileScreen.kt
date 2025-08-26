package com.mehfooz.accounts.app.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.DataThresholding
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as JColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.mehfooz.accounts.app.data.AppDatabase
import com.mehfooz.accounts.app.data.BootstrapManager
import com.mehfooz.accounts.app.net.Downloader
import kotlinx.coroutines.launch
import java.io.File
import java.net.URLEncoder

/* =========================================================
   COLORS — tuned to your app palette
   ========================================================= */
private val DeepBlue = JColor(0xFF0B1E3A)
private val DeepBlueTop = JColor(0xFF0E274E)
private val CardBg = JColor(0xFFF7F9FC)
private val Success  = JColor(0xFF2E7D32)
private val Danger   = JColor(0xFFC62828)
private val Muted    = JColor(0xFF6B7280)

/* =========================================================
   PROFILE SCREEN (polished)
   ========================================================= */
@Composable
fun ProfileScreen(onLogout: () -> Unit) {
    val TAG = "Sync"

    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val user = FirebaseAuth.getInstance().currentUser

    // UI state
    var askConfirm by remember { mutableStateOf(false) }
    var showOtp by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var downloading by remember { mutableStateOf(false) }
    var debugMode by remember { mutableStateOf(false) }
    var txCount by remember { mutableStateOf<Long?>(null) }

    // load current local row count
    LaunchedEffect(Unit) {
        txCount = AppDatabase.get(ctx).transactionsP().count()
    }

    // Top surface keeps your deep-blue background across the whole screen
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars), // plays nice with bottom nav
        color = DeepBlue
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            /* ---------- Header strip ---------- */
            LargeTopBar(
                title = "Profile",
                subtitle = user?.email ?: user?.uid ?: "Signed in",
            )

            /* ---------- Body list ---------- */
            // Use a LazyColumn so everything scrolls smoothly, even while a LinearProgress animates
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp), // 🔧 side spacing; change here
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {

                // Account summary
                item {
                    SectionCard(
                        title = "Account",
                        icon = Icons.Outlined.Storage
                    ) {
                        AccountRow(
                            displayName = user?.email ?: user?.uid ?: "User",
                            rows = txCount,
                            onLogoutClick = { askConfirm = true }
                        )
                    }
                }

                // Local DB (assets) — Import
                item {
                    SectionCard(
                        title = "Local Data",
                        icon = Icons.Outlined.DataThresholding
                    ) {
                        FilledTonalButton(
                            enabled = !downloading,
                            onClick = {
                                scope.launch {
                                    try {
                                        isError = false
                                        status = "Importing local DB…"
                                        downloading = true
                                        progress = 0f
                                        BootstrapManager.importFromAsset(ctx, "live_seed.sqlite")
                                        val total = AppDatabase.get(ctx).transactionsP().count()
                                        txCount = total
                                        status = "Local import complete. Transactions: $total"
                                    } catch (e: Exception) {
                                        isError = true
                                        status = "Local import error: ${(e.message ?: "unknown").take(200)}"
                                        Log.e(TAG, "Local import error", e)
                                    } finally {
                                        downloading = false
                                    }
                                }
                            },
                            contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                        ) {
                            Icon(Icons.Outlined.Storage, contentDescription = null)
                            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                            Text("Load Local DB (assets)")
                        }                    }
                }

                // Server sync
                item {
                    SectionCard(
                        title = "Server Sync",
                        icon = Icons.Outlined.CloudDownload
                    ) {
                        // Debug JSON / Full sync switch
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Settings, contentDescription = null, tint = Muted)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (debugMode) "Debug JSON Mode" else "Full Sync Mode",
                                    color = DeepBlue,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Switch(checked = debugMode, onCheckedChange = { debugMode = it })
                        }

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = { showOtp = true },
                            enabled = !downloading
                        ) {
                            Icon(Icons.Outlined.Key, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Sync from Server")
                        }

                        if (downloading) {
                            Spacer(Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Progress: ${(progress * 100).toInt()}%",
                                color = Muted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Status line (success / error)
                if (!status.isNullOrBlank()) {
                    item {
                        AssistChip(
                            onClick = { /* no-op */ },
                            label = {
                                Text(
                                    status!!,
                                    color = if (isError) Danger else Success,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        )
                    }
                }

                // Dev tools (Log Raw)
                item {
                    SectionCard(
                        title = "Diagnostics",
                        icon = Icons.Outlined.Settings
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch { logRawSamples(ctx, "GraphDebug") }
                            }
                        ) { Text("Log Raw") }
                    }
                }

                // Extra bottom padding so last card doesn't collide with bottom nav
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }

    /* ---------- OTP dialog ---------- */
    if (showOtp) {
        OtpDialog(
            onDismiss = { showOtp = false },
            onVerify = { otp ->
                showOtp = false
                val email = user?.email
                if (email.isNullOrBlank()) {
                    isError = true
                    status = "No email on Google account. Please re‑sign in."
                    return@OtpDialog
                }
                scope.launch {
                    try {
                        isError = false
                        downloading = true
                        progress = 0f

                        val base = "http://kheloaurjeeto.net/Apps/pairing-api/sync/download.php"
                        val qp = buildString {
                            append("email=")
                            append(URLEncoder.encode(email, "UTF-8"))
                            append("&otp=")
                            append(URLEncoder.encode(otp, "UTF-8"))
                            if (debugMode) append("&debug=1")
                        }
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
                            BootstrapManager.bootstrapFromFull(ctx, fullUrl) { pct ->
                                progress = (pct / 100f).coerceIn(0f, 1f)
                            }
                            val total = AppDatabase.get(ctx).transactionsP().count()
                            txCount = total
                            status = "Sync complete. Transactions: $total"
                        }
                    } catch (e: Exception) {
                        isError = true
                        status = "Error: ${(e.message ?: "unknown").take(4000)}"
                        Log.e(TAG, "Sync error", e)
                    } finally {
                        downloading = false
                    }
                }
            }
        )
    }

    /* ---------- Logout confirm ---------- */
    if (askConfirm) {
        AlertDialog(
            onDismissRequest = { askConfirm = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(onClick = { askConfirm = false; onLogout() }) { Text("Yes, sign out") }
            },
            dismissButton = { TextButton(onClick = { askConfirm = false }) { Text("Cancel") } }
        )
    }
}

/* =========================================================
   Reusable bits
   ========================================================= */

@Composable
private fun LargeTopBar(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepBlue)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(title, color = JColor.White, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(2.dp))
        Text(subtitle, color = JColor.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = CardBg),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.large // nice rounded corners
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = DeepBlue)
                Spacer(Modifier.width(8.dp))
                Text(title, color = DeepBlue, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}

@Composable
private fun AccountRow(
    displayName: String,
    rows: Long?,
    onLogoutClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with initials
        val initials = remember(displayName) {
            displayName.trim().take(1).uppercase()
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(DeepBlueTop),
            contentAlignment = Alignment.Center
        ) {
            Text(initials, color = JColor.White, style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(displayName, color = DeepBlue, style = MaterialTheme.typography.titleSmall)
            Text(
                "Local rows: ${rows ?: "…"}",
                color = Muted,
                style = MaterialTheme.typography.bodySmall
            )
        }

        TextButton(onClick = onLogoutClick) {
            Icon(Icons.Outlined.Logout, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Logout")
        }
    }
}

/* ---------- OTP dialog ---------- */
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

/* ---------- tiny DB logs ---------- */
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