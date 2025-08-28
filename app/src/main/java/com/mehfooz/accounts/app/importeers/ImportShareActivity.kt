// app/src/main/java/com/mehfooz/accounts/app/importers/ImportShareActivity.kt
package com.mehfooz.accounts.app.importers

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.mehfooz.accounts.app.MainActivity
import com.mehfooz.accounts.app.data.BootstrapManager
import com.mehfooz.accounts.app.net.ApiResult
import com.mehfooz.accounts.app.net.VerifyAndSyncRequest
import com.mehfooz.accounts.app.net.verifyAndSyncCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ImportShareActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Try to resolve a single shared Uri from different entry points.
        val uri: Uri? = resolveIncomingUri(intent)
        if (uri == null) {
            Toast.makeText(this, "No file received", Toast.LENGTH_SHORT).show()
            finish(); return
        }

        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Please sign in first.", Toast.LENGTH_LONG).show()
            openAppTo("login")
            finish(); return
        }

        lifecycleScope.launch {
            val okToImport = verifyActivation(email = user.email, displayName = user.displayName)
            if (!okToImport) {
                Toast.makeText(this@ImportShareActivity, "Account not activated yet.", Toast.LENGTH_LONG).show()
                openAppTo("activation")
                finish(); return@launch
            }

            // Activated → import the shared DB
            try {
                val tmp = copyToCache(uri)
                BootstrapManager.importFromFile(this@ImportShareActivity, tmp)
                Toast.makeText(this@ImportShareActivity, "Database imported successfully.", Toast.LENGTH_LONG).show()
                openAppTo("overview") // jump to dashboard
            } catch (e: Exception) {
                Toast.makeText(this@ImportShareActivity, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                openAppTo("profile") // optional fallback
            } finally {
                finish()
            }
        }
    }

    /** Resolve a shared file Uri from EXTRA_STREAM, clipData, or data. */
    private fun resolveIncomingUri(intent: Intent?): Uri? {
        if (intent == null) return null

        // 1) ACTION_SEND with EXTRA_STREAM
        (intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))?.let { return it }

        // 2) Multiple share: first item
        intent.clipData?.let { cd ->
            if (cd.itemCount > 0) return cd.getItemAt(0).uri
        }

        // 3) ACTION_VIEW with data
        intent.data?.let { return it }

        return null
    }

    /** Calls verify_and_sync; returns true if enabled + has active subscription. */
    private suspend fun verifyActivation(email: String?, displayName: String?): Boolean {
        val safeEmail = email ?: return false
        val mobileAppId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown-android-id"

        return when (val res = verifyAndSyncCall(
            VerifyAndSyncRequest(
                email = safeEmail,
                mobileAppId = mobileAppId,
                idToken = null, // optional
                appVersion = BuildConfig.VERSION_NAME,
                name = displayName
            )
        )) {
            is ApiResult.Success -> {
                val enabled = (res.body.user?.isEnabled ?: 0) == 1
                val activeSub = res.body.subscription?.hasActive == true
                enabled && activeSub
            }
            is ApiResult.Failure -> false
        }
    }

    private suspend fun copyToCache(uri: Uri): File = withContext(Dispatchers.IO) {
        val name = guessName(uri) ?: "shared.sqlite"
        val out = File(cacheDir, "shared_import_$name").apply { parentFile?.mkdirs() }
        contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Unable to open shared file" }
            out.outputStream().use { output -> input.copyTo(output) }
        }
        out
    }

    private fun guessName(uri: Uri): String? = runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }.getOrNull() ?: uri.lastPathSegment

    /** Bring the main app to a specific route (login/activation/overview/...). */
    private fun openAppTo(route: String) {
        val i = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("navigate", route)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(i)
    }
}