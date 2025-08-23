// SystemBarsForColor.kt
package com.mehfooz.accounts.app.ui

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

/**
 * Colors status & nav bars to [bg] and auto-chooses icon contrast:
 * - dark bg  -> light icons
 * - light bg -> dark icons
 *
 * Requires: androidx.activity 1.8.0+
 */
@Composable
fun SystemBarsForColor(bg: Color) {
    val view = LocalView.current
    if (view.isInEditMode) return

    SideEffect {
        val activity = view.context as? ComponentActivity ?: return@SideEffect
        val argb = bg.toArgb()
        val lightBackground = bg.luminance() > 0.5f

        val statusStyle = if (lightBackground) {
            // Light bar with dark icons
            SystemBarStyle.light(argb, argb)
        } else {
            // Dark bar with light icons
            SystemBarStyle.dark(argb)
        }
        val navStyle = if (lightBackground) {
            SystemBarStyle.light(argb, argb)
        } else {
            SystemBarStyle.dark(argb)
        }

        activity.enableEdgeToEdge(
            statusBarStyle = statusStyle,
            navigationBarStyle = navStyle
        )
    }
}