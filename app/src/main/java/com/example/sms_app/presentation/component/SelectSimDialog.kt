package com.example.sms_app.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

enum class SimType {
    Normal,
    Special,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectSimDialog(
    onDismissRequest: () -> Unit,
    onSend: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    var selectedOption by remember { mutableStateOf(SimType.Normal.name.uppercase()) }
    AlertDialog(
        onDismissRequest = {
            onDismissRequest()
        },
        icon = {
            Icon(Icons.Default.SimCard, null)
        },
        title = {
            Text("Chọn sim")
        },
        text = {
            Column {
                Text("Số lượng 1 | Mẫu tin 1")
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = {
                        expanded = !expanded
                    },
                ) {
                    TextField(
                        enabled = true,
                        readOnly = true,
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                        value = selectedOption,
                        onValueChange = {},
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    )
                    ExposedDropdownMenu(
                        expanded,
                        onDismissRequest = {
                            expanded = false
                        }
                    ) {
                        SimType.entries.map { it.name }.forEach {
                            DropdownMenuItem(
                                text = {
                                    Text(it.uppercase())
                                }, onClick = {
                                    selectedOption = it.uppercase()
                                }
                            )
                        }
                    }
                }

                HorizontalDivider()
                LazyColumn {
                    items(2) { index ->
                        val item = index + 1
                        var check by remember { mutableStateOf(false) }
                        ListItem(
                            leadingContent = {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.SimCard, null)
                                    Text(item.toString(), color = Color.Red)
                                }
                            },
                            headlineContent = {
                                Text("Sim $item".uppercase())
                            },
                            trailingContent = {
                                Checkbox(check, onCheckedChange = {
                                    check = it
                                })
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSend()
            }) {
                Text("Gửi")
            }
        }
    )
}