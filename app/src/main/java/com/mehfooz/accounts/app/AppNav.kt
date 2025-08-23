package com.mehfooz.accounts.app;

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.navigation.compose.*
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector


/* ---- Bottom destinations ---- */
private sealed class Dest(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    data object Home : Dest("home", "Home", Icons.Outlined.Home, Icons.Filled.Home)
    data object Transactions : Dest("tx", "Transactions", Icons.Outlined.Menu, Icons.Filled.Menu)
    data object Profile : Dest("profile", "Profile", Icons.Outlined.Person, Icons.Filled.Person)
}

@Composable
fun AppRoot(onLogout: () -> Unit = {}) {
    val nav = rememberNavController()
    val items = remember { listOf(Dest.Home, Dest.Transactions, Dest.Profile) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val current: NavDestination? = nav.currentBackStackEntryAsState().value?.destination
                items.forEach { dest ->
                    val selected = current?.hierarchy?.any { it.route == dest.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (!selected) nav.navigate(dest.route) {
                                popUpTo(nav.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(if (selected) dest.selectedIcon else dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = Dest.Home.route,
            modifier = Modifier.padding(inner)
        ) {
            // HOME: your existing chart screen stays as-is for now
            composable(Dest.Home.route) {
                DashboardScreen(onLogout = { nav.navigate(Dest.Profile.route); onLogout() })
            }

            // TRANSACTIONS: placeholder for now
            composable(Dest.Transactions.route) {
                PlaceholderScreen(title = "Transactions")
            }

            // PROFILE: we will move sync/login/local-import/etc. here in Step 2
            composable(Dest.Profile.route) {
                PlaceholderScreen(title = "Profile")
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Surface {
        Text(
            text = "$title (placeholder)",
            style = MaterialTheme.typography.headlineSmall
        )
    }
}