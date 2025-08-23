// MainActivity.kt
package com.mehfooz.accounts.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.mehfooz.accounts.app.ui.theme.MehfoozAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Make system bars transparent and let content draw behind them.
        // SystemBarStyle.auto() picks light/dark icons based on system theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        super.onCreate(savedInstanceState)

        setContent {
            MehfoozAppTheme {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val nav = rememberNavController()
    val auth = remember { FirebaseAuth.getInstance() }

    // decide start destination once
    val start = remember { if (auth.currentUser != null) "home" else "login" }

    // react to sign-in / sign-out
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { fb ->
            val user = fb.currentUser
            val current = nav.currentDestination?.route
            if (user == null && current != "login") {
                nav.navigate("login") { popUpTo(0) }
            } else if (user != null && current == "login") {
                nav.navigate("home") { popUpTo(0) }
            }
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    NavHost(navController = nav, startDestination = start) {
        composable("login") {
            LoginScreen(
                onSignedIn = {
                    nav.navigate("home") { popUpTo("login") { inclusive = true } }
                }
            )
        }
        // NEW: bottom tabs shell
        composable("home") {
            HomeTabs(
                onLogout = { FirebaseAuth.getInstance().signOut() }
            )
        }
    }
}