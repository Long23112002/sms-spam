package com.example.sms_app.presentation.component

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sms_app.presentation.viewmodel.PatternViewModel
import kotlinx.coroutines.delay

@Composable
fun PatternDialog(
    patternViewModel: PatternViewModel = hiltViewModel(),
    onDismissRequest: () -> Unit,
) {
    val template = patternViewModel.messageTemplate.observeAsState(listOf()).value
    val default = patternViewModel.default.observeAsState(1).value
    var pattern by remember { mutableStateOf("") }
    pattern = template.firstOrNull { it.id == default }?.content ?: ""
    var patternNum by remember { mutableIntStateOf(default - 1) }
    val context = LocalContext.current

    LaunchedEffect(patternNum) {
        pattern = template.firstOrNull { it.id == patternNum + 1 }?.content ?: ""
    }

    AlertDialog(
        onDismissRequest = { onDismissRequest() },
        icon = { Icon(Icons.Default.Edit, contentDescription = null) },
        title = { Text("Nhập tin mẫu") },
        text = {
            val scrollState = rememberScrollState()

            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 300.dp)
                        .verticalScroll(scrollState)
                        .padding(bottom = 8.dp)
                ) {
                    TextField(
                        value = pattern,
                        onValueChange = { pattern = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Ký tự: ${pattern.length}") }
                    )
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(template.size) { index ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = patternNum == index,
                                onClick = { patternNum = index }
                            )
                            Text("Mẫu ${index + 1}")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                patternViewModel.saveTemplate(pattern, patternNum + 1)
                Toast.makeText(
                    context,
                    "Đã lưu cấu hình template và mẫu mặc định: ${patternNum + 1}",
                    Toast.LENGTH_SHORT
                ).show()
                onDismissRequest()
            }) {
                Text("LƯU")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                pattern = "" // Chỉ xóa nội dung, không đóng dialog
            }) {
                Text("XÓA")
            }
        }
    )
}