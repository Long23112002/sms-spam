package com.example.sms_app.presentation.component

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SimCard

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MenuAnchorType

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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
import com.example.sms_app.utils.SimConfig
import com.example.sms_app.utils.SimManager

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectSimDialog(
    customers: List<com.example.sms_app.data.Customer> = emptyList(),
    patternViewModel: PatternViewModel = hiltViewModel(),
    onDismissRequest: () -> Unit,
    onSend: (MessageTemplate, SimConfig) -> Unit,
) {
    val context = LocalContext.current
    val template = patternViewModel.messageTemplate.observeAsState(listOf()).value
    var showConfirmDialog by remember { mutableStateOf(false) }
    var pendingMessageTemplate by remember { mutableStateOf<MessageTemplate?>(null) }
    var pendingSimConfig by remember { mutableStateOf<SimConfig?>(null) }
    val default = patternViewModel.default.observeAsState(1).value
    val selectedSimId = patternViewModel.selectedSim.observeAsState(1).value

    var expanded by remember { mutableStateOf(false) }
    var selectId by remember { mutableStateOf(TemplateManager.getDefaultTemplates().first()) }
    val availableSims = SimManager.getAvailableSims(context)
    var selectedSim by remember { mutableStateOf(SimInfo()) }
    selectedSim = availableSims.firstOrNull { it.subscriptionId == selectedSimId } ?: availableSims.first()

    var checkedSims by remember { mutableStateOf(setOf(availableSims.firstOrNull()?.subscriptionId ?: -1)) }

    LaunchedEffect(template, default) {
        selectId = runCatching {
            template.first { it.id == default }
        }.getOrDefault(TemplateManager.getDefaultTemplates().first())
    }

    AlertDialog(
        onDismissRequest = { onDismissRequest() },
        icon = { Icon(Icons.Default.SimCard, null) },
        title = { Text("Chọn sim") },
        text = {
            Column {
                Text("Số lượng 1 | Mẫu tin 1")
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                ) {
                    TextField(
                        enabled = true,
                        readOnly = true,
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth(),
                        value = "Mẫu ${selectId.id} - ${selectId.content.take(30)}${if (selectId.content.length > 30) "..." else ""}",
                        onValueChange = {},
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        template.forEach {
                            DropdownMenuItem(
                                text = {
                                    ListItem(
                                        headlineContent = {
                                            Text("Mẫu ${it.id}")
                                        },
                                        supportingContent = {
                                            Text(
                                                it.content.take(50) + if (it.content.length > 50) "..." else "",
                                                maxLines = 2,
                                                fontSize = 12.sp
                                            )
                                        }
                                    )
                                },
                                onClick = {
                                    selectId = it
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
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
                        val isChecked = checkedSims.contains(sim.subscriptionId)

                        ListItem(
                            leadingContent = {
                                Icon(
                                    Icons.Default.SimCard,
                                    contentDescription = "SIM $simSlotIndex",
                                    tint = if (isChecked) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            },
                            headlineContent = {
                                Text(
                                    "SIM $simSlotIndex - ${sim.carrierName.uppercase()}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                                )
                            },
                            supportingContent = {
                                sim.phoneNumber?.let { Text(it.uppercase()) }
                            },
                            trailingContent = {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        checkedSims = if (checked) {
                                            checkedSims + sim.subscriptionId
                                        } else {
                                            checkedSims - sim.subscriptionId
                                        }
                                        if (checkedSims.isNotEmpty()) {
                                            selectedSim = availableSims.find { it.subscriptionId in checkedSims } ?: sim
                                        }
                                    }
                                )
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val selectedSimsList = availableSims.filter { it.subscriptionId in checkedSims }
                val simConfig = when {
                    selectedSimsList.size >= 2 -> {
                        SimConfig(
                            isDualSim = true,
                            primarySim = selectedSimsList[0],
                            secondarySim = selectedSimsList.getOrNull(1),
                            allSims = selectedSimsList
                        )
                    }
                    selectedSimsList.size == 1 -> {
                        val sim = selectedSimsList[0]
                        SimConfig(
                            isDualSim = false,
                            primarySim = sim,
                            secondarySim = null,
                            allSims = listOf(sim)
                        )
                    }
                    else -> {
                        val fallback = availableSims.firstOrNull() ?: SimInfo()
                        SimConfig(
                            isDualSim = false,
                            primarySim = fallback,
                            secondarySim = null,
                            allSims = listOf(fallback)
                        )
                    }
                }

                pendingMessageTemplate = selectId
                pendingSimConfig = simConfig
                showConfirmDialog = true
            }) {
                Text("Gửi")
            }
        }
    )

    if (showConfirmDialog && pendingMessageTemplate != null && pendingSimConfig != null) {
        val selectedCustomers = customers.filter { it.isSelected }

        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
                pendingMessageTemplate = null
                pendingSimConfig = null
            },
            title = { Text("Xác nhận gửi tin nhắn") },
            text = {
                Column {
                    Text("Bạn có chắc chắn muốn gửi tin nhắn đến:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${selectedCustomers.size} khách hàng",
                        fontWeight = FontWeight.Bold,
                        color = Color.Blue
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Mẫu tin: ${pendingMessageTemplate!!.content.take(100)}${if (pendingMessageTemplate!!.content.length > 100) "..." else ""}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSend(pendingMessageTemplate!!, pendingSimConfig!!)
                    showConfirmDialog = false
                    pendingMessageTemplate = null
                    pendingSimConfig = null
                }) {
                    Text("Đồng ý gửi", color = Color.Blue)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    pendingMessageTemplate = null
                    pendingSimConfig = null
                }) {
                    Text("Hủy")
                }
            }
        )
    }
}