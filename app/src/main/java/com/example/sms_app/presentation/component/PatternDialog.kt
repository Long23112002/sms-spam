package com.example.sms_app.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun PatternDialog(
    onDismissRequest: () -> Unit,
) {
    var pattern by remember {
        mutableStateOf("")
    }
    var patternNum by remember {
        mutableIntStateOf(0)
    }
    AlertDialog(
        onDismissRequest = {
            onDismissRequest()
        },
        icon = {
            Icon(Icons.Default.Edit, null)
        },
        title = {
            Text("Nhập tin mẫu")
        },
        text = {
            Column {
                TextField(
                    value = pattern,
                    onValueChange = {
                        pattern = it
                    },
                    Modifier.fillMaxWidth(),
                    label = {
                        Text("Ký tự: ${pattern.length} | Trang: ${pattern.length / 10 + 1}")
                    }
                )
                LazyRow {
                    items(5) { index ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                patternNum == index,
                                onClick = {
                                    patternNum = index
                                }
                            )
                            Text("Mẫu ${index + 1}")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onDismissRequest()
            }) {
                Text("Lưu".uppercase())
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismissRequest()
            }) {
                Text("Xóa".uppercase())
            }
        }
    )
}