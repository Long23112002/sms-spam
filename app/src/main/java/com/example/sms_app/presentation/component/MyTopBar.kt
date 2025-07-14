package com.example.sms_app.presentation.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTopBar(
    selectAll: Boolean,
    onDeleteAll: () -> Unit,
    onUpload: () -> Unit,
    onCheckedChange: ((Boolean) -> Unit)
) {
    var showDeleteDialog by remember {
        mutableStateOf(false)
    }
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = selectAll,
                    onCheckedChange = { isChecked ->
                        onCheckedChange(isChecked)
                    }
                )
                Text("Chọn tất cả")
            }
        },
        modifier = Modifier,
        actions = {
            OutlinedButton(
                onClick = {
                    showDeleteDialog = true
                }
            ) {
                Icon(Icons.Default.DeleteForever, null)
            }
            Spacer(Modifier.width(4.dp))
            OutlinedButton(
                onClick = {
                    onUpload()
                }
            ) {
                Icon(Icons.Default.FileUpload, null)
            }
        },
    )
    
    // Confirmation dialog for delete all
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text("Xác nhận xóa")
            },
            text = {
                Text("Bạn có chắc chắn muốn xóa tất cả khách hàng không? Hành động này không thể hoàn tác.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteAll()
                    }
                ) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Hủy")
                }
            }
        )
    }
}