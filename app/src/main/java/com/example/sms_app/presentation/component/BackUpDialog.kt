package com.example.sms_app.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sms_app.presentation.viewmodel.BackUpViewModel
import java.text.SimpleDateFormat
import java.util.Date


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackUpDialog(backUpViewModel: BackUpViewModel = hiltViewModel(), onDismissRequest: () -> Unit) {
    val date = SimpleDateFormat("dd-MM-yyyy", java.util.Locale("vi")).format(Date())
    var name by remember {
        mutableStateOf("${date}_${System.currentTimeMillis()}.csv")
    }
    var isConfirm by remember {
        mutableStateOf(false)
    }
    var msg by remember {
        mutableStateOf("")
    }
    BasicAlertDialog(onDismissRequest = onDismissRequest, modifier = Modifier.padding(8.dp)) {
        Card {
            Column(Modifier.padding(4.dp)) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Sao lưu")
                    HorizontalDivider()
                    Text(
                        "Danh sách sẽ được sao lưu tại thư mục",
                        style = TextStyle(fontSize = 14.sp, textAlign = TextAlign.Center)
                    )
                    Text(
                        "Download/SMSS/$name",
                        style = TextStyle(fontSize = 14.sp, textAlign = TextAlign.Center)
                    )
                }

                TextField(name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = {
                    backUpViewModel.backUp(
                        name,
                        onResponse = {
                            isConfirm = true
                            msg = it
                        }
                    )
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Bắt đầu")
                }
            }
        }
    }

    if (isConfirm) {
        AlertDialog(
            onDismissRequest = {
                isConfirm = false
            },
            icon = {
                Icon(Icons.Default.Info, null)
            },
            title = {
                Text("Thông báo")
            },
            text = {
                Text(msg)
            },
            confirmButton = {
                TextButton(onClick = {
                    isConfirm = false
                    onDismissRequest()
                }) {
                    Text("Đóng")
                }
            })
    }
}