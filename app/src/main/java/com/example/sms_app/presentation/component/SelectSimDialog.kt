package com.example.sms_app.presentation.component

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sms_app.data.MessageTemplate
import com.example.sms_app.data.TemplateManager
import com.example.sms_app.presentation.viewmodel.PatternViewModel
import com.example.sms_app.presentation.viewmodel.SettingViewModel
import com.example.sms_app.utils.SimInfo
import com.example.sms_app.utils.SimManager

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectSimDialog(
    patternViewModel: PatternViewModel = hiltViewModel(),
    onDismissRequest: () -> Unit,
    onSend: (MessageTemplate, SimInfo) -> Unit,
) {
    val context = LocalContext.current
    val template = patternViewModel.messageTemplate.observeAsState(listOf()).value
    val default = patternViewModel.default.observeAsState(1).value
    val selectedSimId = patternViewModel.selectedSim.observeAsState(1).value

    var expanded by remember { mutableStateOf(false) }
    var selectId by remember { mutableStateOf(TemplateManager.getDefaultTemplates().first()) }
    val availableSims = SimManager.getAvailableSims(context)
    var selectedSim by remember { mutableStateOf(SimInfo()) }
    selectedSim = availableSims.firstOrNull { it.subscriptionId == selectedSimId } ?: availableSims.first()
    LaunchedEffect(template, default) {
        selectId = runCatching {
            template.first { it.id == default }
        }.getOrDefault(TemplateManager.getDefaultTemplates().first())
    }

    var check by remember { mutableIntStateOf(selectedSim.subscriptionId) }
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
                        value = "Mẫu ${selectId.id} - ${selectId.content}",
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
                        template.forEach {
                            DropdownMenuItem(
                                text = {
                                    ListItem(
                                        headlineContent = {
                                            Text("Mẫu ${it.id}")
                                        },
                                        supportingContent = {
                                            Text(it.content)
                                        }
                                    )
                                }, onClick = {
                                    selectId = it
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                HorizontalDivider()
                LazyColumn {
                    items(availableSims.size) { index ->
                        val sim = availableSims[index]
                        val simSlotIndex = sim.simSlotIndex + 1
                        val carrierColor = when (sim.carrierName) {
                            "Viettel" -> Color(0xFF4CAF50)
                            "Mobifone" -> Color(0xFF2196F3)
                            "Vinaphone" -> Color(0xFFF44336)
                            "Vietnamobile" -> Color(0xFF607D8B)
                            else -> Color(0xFF9C27B0)
                        }
                        ListItem(
                            leadingContent = {
                                Box(
                                    Modifier
                                        .background(carrierColor, RoundedCornerShape(20))
                                        .width(50.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(sim.carrierName)
                                }
                            },
                            supportingContent = {
                                Text("${sim.phoneNumber}".uppercase())
                            },
                            headlineContent = {
                                Text(
                                    "Sim $simSlotIndex - ${sim.carrierName}".uppercase(),
                                    style = TextStyle(fontSize = 14.sp)
                                )
                            },
                            trailingContent = {
                                RadioButton(check == sim.subscriptionId, onClick = {
                                    check = sim.subscriptionId
                                    selectedSim = sim
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
                onSend(selectId, selectedSim)
            }) {
                Text("Gửi")
            }
        }
    )
}