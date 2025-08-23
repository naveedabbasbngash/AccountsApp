// file: com/mehfooz/accounts/app/ui/util/ModifierExt.kt
package com.mehfooz.accounts.app.ui.util

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

fun Modifier.noRippleClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    this.then(
        Modifier.clickable(
            enabled = enabled,
            indication = null,
            interactionSource = interaction,
            onClick = onClick
        )
    )
}