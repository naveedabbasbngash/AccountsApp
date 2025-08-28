// MainActivity.kt
package com.mehfooz.accounts.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.mehfooz.accounts.app.ui.OverviewScreen
import com.mehfooz.accounts.app.ui.ProfileScreen
import com.mehfooz.accounts.app.ui.TransactionsScreen
import com.mehfooz.accounts.app.ui.theme.MehfoozAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Transparent edge-to-edge system bars
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
        setContent { MehfoozAppTheme { AppRoot() } }
    }
}

/* ---------------- Navigation model ---------------- */

private object Routes {
    const val LOGIN = "login"
    const val ACTIVATION = "activation"
    const val OVERVIEW = "overview"
    const val TRANSACTIONS = "transactions"
    const val PROFILE = "profile"
}

/** Bottom destinations we want tabs for */
private data class BottomDest(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomDests = listOf(
    BottomDest(Routes.OVERVIEW, "Overview", Icons.Outlined.Assessment),
    BottomDest(Routes.TRANSACTIONS, "Transactions", Icons.Outlined.ListAlt),
    BottomDest(Routes.PROFILE, "Profile", Icons.Outlined.AccountCircle),
)

/* ---------------- Root ---------------- */

@Composable
private fun AppRoot() {
    val nav = rememberNavController()

    // Show bottom bar only on these routes
    val bottomRoutes = remember { bottomDests.map { it.route }.toSet() }

    val backStack by nav.currentBackStackEntryAsState()
    val currentDest = backStack?.destination

    val showBottomBar = currentDest?.route in bottomRoutes

    Scaffold(
        // ✅ prevent white background under system bars
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),

        bottomBar = {
            if (showBottomBar) {
                BottomBar(
                    currentDestination = currentDest,
                    onClick = { route ->
                        nav.navigate(route) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(nav.graph.startDestinationId) { saveState = true }
                        }
                    }
                )
            }
        }
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = Routes.LOGIN,
            modifier = Modifier.padding(inner)
        ) {
            // ---- Auth flow ----
            composable(Routes.LOGIN) {
                LoginScreen(
                    onGoDashboard = {
                        nav.navigate(Routes.OVERVIEW) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onActivationRequired = {
                        nav.navigate(Routes.ACTIVATION) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Routes.ACTIVATION) {
                ActivationScreen(
                    onRefreshCheck = {
                        nav.popBackStack()
                        nav.navigate(Routes.LOGIN) { launchSingleTop = true }
                    }
                )
            }

            // ---- Main (tab) destinations ----
            composable(Routes.OVERVIEW) { OverviewScreen() }
            composable(Routes.TRANSACTIONS) { TransactionsScreen() }
            composable(Routes.PROFILE) {
                val auth = remember { FirebaseAuth.getInstance() }
                ProfileScreen(
                    onLogout = {
                        auth.signOut()
                        nav.navigate(Routes.LOGIN) {
                            popUpTo(0)
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}

/* ---------------- Bottom Bar ---------------- */

@Composable
private fun BottomBar(
    currentDestination: NavDestination?,
    onClick: (String) -> Unit
) {
    NavigationBar {
        bottomDests.forEach { dest ->
            val selected = currentDestination
                ?.hierarchy
                ?.any { it.route == dest.route } == true

            NavigationBarItem(
                selected = selected,
                onClick = { onClick(dest.route) },
                icon = { Icon(dest.icon, contentDescription = dest.label) },
                label = { Text(dest.label) }
            )
        }
    }
}

/* ---------------- Activation screen ---------------- */

@Composable
fun ActivationScreen(onRefreshCheck: () -> Unit) {
    val DeepBlue = MaterialTheme.colorScheme.primary.copy(alpha = 0.95f)
    val Muted = MaterialTheme.colorScheme.onSurfaceVariant
    val Success = Color(0xFF2E7D32)

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
            // Card centered on screen
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
                        "Account Pending Activation",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = DeepBlue
                    )

                    Text(
                        "Your account is not activated yet. Please wait until an admin enables your access. Once activated, you can continue to the dashboard.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Muted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = onRefreshCheck,
                        colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Check Activation", color = Color.White)
                    }

                    Text(
                        "Tip: You may close and reopen the app anytime. We’ll check your activation automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}