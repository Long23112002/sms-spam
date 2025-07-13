package com.example.sms_app.presentation.component

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sms_app.MainActivity.SmsProgress
import com.example.sms_app.data.MessageTemplate
import com.example.sms_app.presentation.viewmodel.SendMessageViewModel
import com.example.sms_app.utils.SimInfo


enum class BottomButton(val icon: ImageVector) {
    Add(Icons.Default.PersonAdd),
    Send(Icons.AutoMirrored.Filled.Send),
    Message(Icons.AutoMirrored.Filled.Message),

    Setting(Icons.Default.Settings),
    BackUp(Icons.Default.Download),
    Search(Icons.Default.Search),
    MoreVert(Icons.Default.MoreVert),
    None(Icons.Default.MoreVert),

    SendMessage(Icons.Default.MoreVert),
    WrongPattern(Icons.Default.MoreVert),
    ;
}

@Composable
fun MyBottomBar(
    providers: List<String>,
    sendMessageViewModel: SendMessageViewModel = hiltViewModel(),
    onBottomButton: ((BottomButton) -> Unit),
    onProviderSelected: ((String) -> Unit) = {},
    onCustomerAdded: (() -> Unit) = {}
) {
    var button by remember {
        mutableStateOf(BottomButton.None)
    }
    var messageTemplate by remember {
        mutableStateOf(MessageTemplate())
    }
    var simInfo by remember {
        mutableStateOf(SimInfo())
    }
    val progress = sendMessageViewModel.progress.observeAsState(SmsProgress()).value
    val completion = sendMessageViewModel.completion.observeAsState().value
    val isSending = sendMessageViewModel.isSending.observeAsState().value
    val context = LocalContext.current

    LaunchedEffect(completion) {
        completion?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    BottomAppBar(Modifier.height(210.dp)) {
        Column {
            if (isSending == true) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    progress.let {
                        Text("${it.progress}/${it.total}")
                        TextButton(onClick = {
                            button = BottomButton.SendMessage
                        }) {
                            Text("Open dialog")
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(
                    listOf(BottomButton.Setting, BottomButton.BackUp),
                    listOf(BottomButton.Search, BottomButton.MoreVert),
                ).forEach {
                    Row {
                        it.forEach { x ->
                            IconButton(onClick = {
                                button = x
                                onBottomButton(x)
                            }) {
                                Icon(x.icon, contentDescription = null)
                            }
                            if (button == BottomButton.MoreVert && x == BottomButton.MoreVert) {
                                MoreView(button) {
                                    button = BottomButton.None
                                }
                            }
                        }
                    }
                }
            }
            HorizontalDivider()
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                var selectedProvider by remember {
                    mutableStateOf("all")
                }
                Text("Lọc theo nhà mạng")
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedProvider == "all",
                            onClick = { 
                                selectedProvider = "all"
                                onProviderSelected("all")
                            }
                        )
                        Text("Tất cả")
                    }

                    providers.forEach { provider ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedProvider == provider,
                                onClick = { 
                                    selectedProvider = provider
                                    onProviderSelected(provider)
                                }
                            )
                            Text(provider.capitalize(Locale.current))
                        }
                    }
                }
            }
            HorizontalDivider()
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf(BottomButton.Add, BottomButton.Send, BottomButton.Message).forEach {
                    IconButton(onClick = {
                        button = it
                        onBottomButton(it)
                    }) {
                        Icon(it.icon, contentDescription = null)
                    }
                }
            }
        }
    }

    when (button) {
        BottomButton.Add -> {
            AddCustomerDialog(
                onDismissRequest = {
                    button = BottomButton.None
                },
                onCustomerAdded = {
                    onCustomerAdded()
                }
            )
        }

        BottomButton.Send -> {
            SelectSimDialog(onDismissRequest = {
                button = BottomButton.None
            }, onSend = { t, s ->
                messageTemplate = t
                simInfo = s
                button = BottomButton.SendMessage
            })
        }

        BottomButton.Message -> {
            PatternDialog {
                button = BottomButton.None
            }
        }

        BottomButton.Setting -> {
            SettingDialog {
                button = BottomButton.None
            }
        }

        BottomButton.BackUp -> {
            BackUpDialog {
                button = BottomButton.None
            }
        }

        BottomButton.Search -> {}
        BottomButton.MoreVert -> {}
        BottomButton.None -> {}
        BottomButton.SendMessage -> {
            SendMessageDialog(
                messageTemplate,
                simInfo,
                sendMessageViewModel
            ) {
                button = BottomButton.None
            }
        }

        BottomButton.WrongPattern -> {
            WrongPatternDialog(
                onDismissRequest = {
                    button = BottomButton.None
                },
                confirm = {},
                continueMessage = {}
            )
        }
    }
}
