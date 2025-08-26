package com.mehfooz.accounts.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mehfooz.accounts.app.ui.OverviewScreen
import com.mehfooz.accounts.app.ui.ProfileScreen
import com.mehfooz.accounts.app.ui.TransactionsScreen

/* ---------------- Bottom destinations ---------------- */
private sealed class Dest(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    data object Overview : Dest("overview", "Overview", Icons.Outlined.Home, Icons.Filled.Home)
    data object Transactions : Dest("transactions", "Transactions", Icons.Outlined.Menu, Icons.Filled.Menu)
    data object Profile : Dest("profile", "Profile", Icons.Outlined.Person, Icons.Filled.Person)
}

/* ---------------- App root with bottom navigation ---------------- */
@Composable
fun AppRoot(onLogout: () -> Unit = {}) {
    val nav = rememberNavController()
    val items = listOf(Dest.Overview, Dest.Transactions, Dest.Profile)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by nav.currentBackStackEntryAsState()
                val current: NavDestination? = backStackEntry?.destination

                items.forEach { dest ->
                    val selected = current?.hierarchy?.any { it.route == dest.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (!selected) {
                                nav.navigate(dest.route) {
                                    // keep one instance per tab + restore last state when reselecting
                                    popUpTo(nav.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(if (selected) dest.selectedIcon else dest.icon, dest.label) },
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = nav,
            startDestination = Dest.Overview.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // OVERVIEW (your dashboard/graphs)
            composable(Dest.Overview.route) {
                OverviewScreen()
            }

            // TRANSACTIONS (your new list with search + filters)
            composable(Dest.Transactions.route) {
                TransactionsScreen()
            }

            // PROFILE (sync/import/logout)
            composable(Dest.Profile.route) {
                ProfileScreen(onLogout = onLogout)
            }
        }
    }
}