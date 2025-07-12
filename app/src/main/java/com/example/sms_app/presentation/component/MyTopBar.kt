package com.example.sms_app.presentation.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
    onSync: () -> Unit,
    onDeleteAll: () -> Unit,
    onUpload: () -> Unit,
    onCheckedChange: ((Boolean) -> Unit)
) {
    var selectAll by remember {
        mutableStateOf(false)
    }
    TopAppBar(
        title = {},
        modifier = Modifier,
        actions = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    selectAll,
                    onCheckedChange = {
                        selectAll = it
                        onCheckedChange(it)
                    }
                )
                Text("Chọn tất cả")
            }
            Spacer(Modifier.width(10.dp))
            OutlinedButton(
                onClick = {
                    onDeleteAll()
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
            Spacer(Modifier.width(4.dp))
            OutlinedButton(onClick = { onSync() }) {
                Icon(Icons.Default.Sync, null)
            }
        },
    )
}