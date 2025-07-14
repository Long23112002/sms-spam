package com.example.sms_app.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


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
                    onCheckedChange = { isChecked -> onCheckedChange(isChecked) },
                    modifier = Modifier.size(16.dp) // chỉnh size nhỏ hơn bình thường
                )
                Spacer(modifier = Modifier.width(6.dp)) // khoảng cách nhỏ giữa checkbox và text
                Text(
                    text = "Chọn tất cả",
                    fontSize = 15.sp // giảm font size
                )
            }
        },
        modifier = Modifier,
        actions = {
            Button(
                onClick = { showDeleteDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent // Nền trong suốt
                ),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, Color.Red), // Viền đỏ
                modifier = Modifier
//                    .fillMaxWidth(0.95f) // gần full chiều ngang, có thể điều chỉnh
                    .height(50.dp)
            ) {
                Text(
                    text = "Xóa tất cả",
                    color = Color.Red,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.width(8.dp))

            // Nút Chọn tệp tin - màu xanh dương
            Button(
                onClick = {
                    onUpload()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, Color.Magenta),
                modifier = Modifier.height(50.dp)
            ) {
                Text(
                    "Chọn tệp tin",
                    color = Color.Magenta,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
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