package com.mehfooz.accounts.app

import android.app.Activity
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.mikephil.charting.BuildConfig
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.mehfooz.accounts.app.net.ApiResult
import com.mehfooz.accounts.app.net.VerifyAndSyncRequest
import com.mehfooz.accounts.app.net.verifyAndSyncCall
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onGoDashboard: () -> Unit,
    onActivationRequired: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    val auth = remember { FirebaseAuth.getInstance() }
    val scope = rememberCoroutineScope()

    // ✅ Use your app's BuildConfig (not MPAndroidChart)
    val appVersion = remember { BuildConfig.VERSION_NAME }

    // Stable device id to send as "mobile_app_id"
    val mobileAppId = remember {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown-android-id"
    }

    // Google Sign-In client
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(activity.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    val googleClient: GoogleSignInClient = remember { GoogleSignIn.getClient(activity, gso) }

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    /** Call backend to verify/sync after Firebase sign-in */
    fun verifyWithServer(
        email: String,
        idToken: String?,
        displayName: String?
    ) {
        scope.launch {
            loading = true
            error = null
            val result = verifyAndSyncCall(
                VerifyAndSyncRequest(
                    email = email,
                    mobileAppId = mobileAppId,
                    idToken = idToken,
                    appVersion = appVersion,
                    name = displayName
                )
            )
            loading = false
            when (result) {
                is ApiResult.Success -> {
                    val body = result.body
                    val isEnabled = (body.user?.isEnabled ?: 0) == 1
                    val hasActive = body.subscription?.hasActive == true
                    if (isEnabled && hasActive) onGoDashboard() else onActivationRequired()
                }
                is ApiResult.Failure -> {
                    error = result.message ?: "Server error (code ${result.code ?: "?"})"
                }
            }
        }
    }

    // Google activity result launcher
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            loading = true
            auth.signInWithCredential(credential)
                .addOnSuccessListener {
                    loading = false
                    val email = account.email ?: auth.currentUser?.email
                    if (email.isNullOrBlank()) {
                        error = "No email returned from Google."
                    } else {
                        verifyWithServer(
                            email = email,
                            idToken = account.idToken,
                            displayName = account.displayName
                        )
                    }
                }
                .addOnFailureListener { e ->
                    loading = false
                    error = e.message ?: "Sign-in failed"
                }
        } catch (e: Exception) {
            loading = false
            error = e.message ?: "Google sign-in cancelled/failed"
        }
    }

    // Auto-skip for returning users
    LaunchedEffect(Unit) {
        val user = auth.currentUser
        if (user != null && !loading) {
            verifyWithServer(
                email = user.email ?: return@LaunchedEffect,
                idToken = null,
                displayName = user.displayName
            )
        }
    }

    // ---------- UI (Polished like Activation screen) ----------
    val DeepBlue = MaterialTheme.colorScheme.primary.copy(alpha = 0.95f)
    val Muted = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DeepBlue
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ElevatedCard(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountCircle,
                        contentDescription = null,
                        tint = DeepBlue,
                        modifier = Modifier.size(64.dp)
                    )

                    Text(
                        text = "Welcome to Mehfooz Accounts",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = DeepBlue,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Sign in with your Google account to continue. We’ll verify your subscription & activation automatically.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Muted,
                        textAlign = TextAlign.Center
                    )

                    // Sign-in button
                    Button(
                        onClick = {
                            error = null
                            loading = true
                            launcher.launch(googleClient.signInIntent)
                        },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepBlue)
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier
                                    .size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Signing in…", color = Color.White)
                        } else {
                            Text("Continue with Google", color = Color.White)
                        }
                    }

                    // Error banner
                    if (!error.isNullOrBlank()) {
                        AssistChip(
                            onClick = { /* no-op */ },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        error!!,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        )
                    }

                    Divider()

                    Text(
                        text = "Returning users skip this screen automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "App version $appVersion",
                        style = MaterialTheme.typography.labelSmall,
                        color = Muted.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}