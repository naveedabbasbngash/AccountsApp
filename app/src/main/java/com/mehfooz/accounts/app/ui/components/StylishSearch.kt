package com.mehfooz.accounts.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@Composable
fun StylishSearchOverlay(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    suggestions: List<String>,
    onSuggestionPicked: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val view = LocalView.current
    var expanded by remember { mutableStateOf(false) }

    // keep track of TextField height
    var textFieldHeight by remember { mutableStateOf(0) }

    Column(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = it.text.isNotBlank() && suggestions.isNotEmpty()
            },
            placeholder = { Text("Search…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (value.text.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        modifier = Modifier
                            .clickable {
                                onValueChange(TextFieldValue(""))
                                expanded = false
                            }
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onGloballyPositioned { coords ->
                    textFieldHeight = coords.size.height
                }
        )

        if (expanded) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, textFieldHeight), // shift dropdown below box
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = false)
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .background(Color.White)
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                    ) {
                        items(suggestions) { name ->
                            DropdownMenuItem(
                                text = { Text(name, color = Color(0xFF0B1E3A)) },
                                onClick = {
                                    val end = name.length
                                    onValueChange(TextFieldValue(name, selection = TextRange(end)))
                                    onSuggestionPicked(name)
                                    expanded = false
                                    focusManager.clearFocus(force = true)
                                    view.post { focusRequester.requestFocus() }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}