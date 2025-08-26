package com.mehfooz.accounts.app

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mehfooz.accounts.app.ui.OverviewScreen
import com.mehfooz.accounts.app.ui.ProfileScreen
import com.mehfooz.accounts.app.ui.TransactionsScreen

@Composable
fun HomeTabs(
    onLogout: () -> Unit
) {
    val nav = rememberNavController()
    val items = listOf(
        BottomItem("overview", "Overview", Icons.Outlined.Dashboard),
        BottomItem("transactions", "Transactions", Icons.Outlined.Payments),
        BottomItem("profile", "Profile", Icons.Outlined.Person)
    )

    val deepBlue = Color(0xFF0B1E3A)

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
        },
        containerColor = deepBlue,          // <- color behind your screens
        contentWindowInsets = WindowInsets(0) // <- we’ll consume inner padding ourselves
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = "overview",
            modifier = Modifier
                .padding(inner)                // <- apply Scaffold’s padding (removes the bottom gap)
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

private fun NavGraphBuilder.addOverviewTab() {
    composable("overview") {
        // IMPORTANT: OverviewScreen should accept a Modifier and NOT add its own nav-bar padding.
        OverviewScreen()
    }
}

private fun NavGraphBuilder.addTransactionsTab() {
    composable("transactions") {
        TransactionsScreen()
    }
}

private fun NavGraphBuilder.addProfileTab(onLogout: () -> Unit) {
    composable("profile") {
        ProfileScreen(onLogout = onLogout)
    }
}