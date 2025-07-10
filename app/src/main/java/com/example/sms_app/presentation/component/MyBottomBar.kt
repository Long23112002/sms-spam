package com.example.sms_app.presentation.component

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp


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
fun MyBottomBar(providers: List<String>, onBottomButton: ((BottomButton) -> Unit)) {
    var button by remember {
        mutableStateOf(BottomButton.None)
    }
    BottomAppBar(Modifier.height(170.dp)) {
        Column {
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
                var all by remember {
                    mutableStateOf(true)
                }
                Text("Lọc theo nhà mạng")
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = all, onClick = { all = !all })
                        Text("Tất cả")
                    }

                    providers.forEach {
                        var selected by remember {
                            mutableStateOf(false)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selected || all,
                                onClick = { selected = !selected })
                            Text(it.capitalize(Locale.current))
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
            AddCustomerDialog {
                button = BottomButton.None
            }
        }

        BottomButton.Send -> {
            SelectSimDialog(onDismissRequest = {
                button = BottomButton.None
            }, onSend = {
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
            SendMessageDialog {
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
