package com.mehfooz.accounts.app

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun OtpDialog(
    onDismiss: () -> Unit,
    onVerify: (String) -> Unit,
    title: String = "Enter OTP",
    hint: String = "6-digit code"
) {
    var otp by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = otp,
                    onValueChange = {
                        otp = it.trim()
                        error = null
                    },
                    label = { Text(hint) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    isError = error != null
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (otp.isBlank()) { error = "OTP cannot be empty"; return@TextButton }
                if (otp.length < 4) { error = "OTP looks too short"; return@TextButton } // change to 6 if needed
                onVerify(otp)
            }) { Text("Verify") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}