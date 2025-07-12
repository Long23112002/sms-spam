package com.example.sms_app.presentation.component

import android.annotation.SuppressLint
import android.os.CountDownTimer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sms_app.data.AppSettings
import com.example.sms_app.data.MessageTemplate
import com.example.sms_app.presentation.viewmodel.PatternViewModel
import com.example.sms_app.presentation.viewmodel.SendMessageViewModel
import com.example.sms_app.presentation.viewmodel.SettingViewModel
import com.example.sms_app.utils.SimInfo
import com.example.sms_app.utils.SimManager

@SuppressLint("MissingPermission")
@Composable
fun SendMessageDialog(
    messageTemplate: MessageTemplate,
    simInfo: SimInfo,
    sendMessageViewModel: SendMessageViewModel,
    onDismissRequest: () -> Unit
) {
    val time = remember {
        mutableStateListOf<Long>().apply {
            for (i in 0..3) {
                add(0)
            }
        }
    }
    val millisUntilFinished = sendMessageViewModel.millisUntilFinished.observeAsState(0).value

    time.apply {
        clear()
        addAll(millisUntilFinished.formatDuration())
    }

    LaunchedEffect(Unit) {
        sendMessageViewModel.sendMessage(messageTemplate, simInfo)
    }
    AlertDialog(
        onDismissRequest = { onDismissRequest() },
        icon = {
            CircularProgressIndicator()
        },
        title = {
            Text("Đang gửi tin..")
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row {
                    Icon(Icons.Default.SimCard, null)
                    Text("sim ${simInfo.simSlotIndex + 1}".uppercase())
                }
                Spacer(Modifier.height(10.dp))
                Row {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cake, null)
                        Text("Normal".uppercase())
                    }
                    Spacer(Modifier.width(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Message, null)
                        Text("mẫu tin ${messageTemplate.id}".uppercase())
                    }
                }
                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    time.forEachIndexed { id, item ->
                        Box(
                            Modifier
                                .background(Color.Black, shape = RoundedCornerShape(20)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "%02d".format(item),
                                color = Color.White,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        if (id < time.size - 1) {
                            Text(":", modifier = Modifier.padding(10.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismissRequest() }) {
                Text("Đóng")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                sendMessageViewModel.stop()
                onDismissRequest()
            }) {
                Text("Dừng gửi")
            }
        }
    )
}

fun Long.formatDuration(): List<Long> {
    val seconds = this / 1000
    val days = seconds / (24 * 3600)
    val hours = (seconds % (24 * 3600)) / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return listOf(
        days,
        hours,
        minutes,
        secs
    )
}