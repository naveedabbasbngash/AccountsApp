package com.mehfooz.accounts.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mehfooz.accounts.app.ui.OverviewScreen
import com.mehfooz.accounts.app.ui.ProfileScreen

@Composable
fun HomeTabs(
    onLogout: () -> Unit
) {
    val nav = rememberNavController()

    val items = listOf(
        BottomItem("overview", "Overview", Icons.Outlined.Place),
        BottomItem("transactions", "Transactions", Icons.Outlined.LocationOn),
        BottomItem("profile", "Profile", Icons.Outlined.Person)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStack by nav.currentBackStackEntryAsState()
                val current = backStack?.destination?.route
                items.forEach { item ->
                    NavigationBarItem(
                        selected = current == item.route,
                        onClick = {
                            if (current != item.route) {
                                nav.navigate(item.route) {
                                    popUpTo(nav.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = "overview",
            modifier = Modifier.padding(inner)
        ) {
            addOverviewTab()
            addTransactionsTab()
            addProfileTab(onLogout)
        }
    }
}

private data class BottomItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

// --- tabs ---
private fun NavGraphBuilder.addOverviewTab() {
    composable("overview") {
        // Move your GRAPH content here (from DashboardScreen)
        OverviewScreen()
    }
}

private fun NavGraphBuilder.addTransactionsTab() {
    composable("transactions") {
        // placeholder for now
        Text("Transactions (coming soon)")
    }
}

private fun NavGraphBuilder.addProfileTab(onLogout: () -> Unit) {
    composable("profile") {
        // Move your Sync / Import / Debug controls here
        ProfileScreen(onLogout = onLogout)
    }
}