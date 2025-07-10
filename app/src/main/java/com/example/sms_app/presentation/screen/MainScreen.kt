package com.example.sms_app.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sms_app.data.Customer
import com.example.sms_app.presentation.component.MyBottomBar
import com.example.sms_app.presentation.component.MyTopBar
import com.example.sms_app.presentation.viewmodel.MainViewModel

@Composable
fun MainScreen(mainViewModel: MainViewModel = hiltViewModel()) {
    val providers = listOf("viettel", "mobifone", "vinaphone")
    var selectAll by remember {
        mutableStateOf(false)
    }
    val customers = buildList {
        for (i in 0..99) {
            add(
                Customer(
                    id = "",
                    name = "Hxt",
                    phoneNumber = "03462222222",
                )
            )
        }
    }

//    val customers: List<Customer> = listOf()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            MyTopBar {
                selectAll = it
            }
        },
        bottomBar = {
            MyBottomBar(providers) {

            }
        }
    ) { paddingValues ->
        Column(
            Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            val headers = listOf("tên khách hàng", "số điện thoại", "chọn", "xóa")
            val weights = listOf(3f, 2f, 1f, 1f)
            HorizontalDivider()
            Row(
                Modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                headers.forEachIndexed { id, item ->
                    Text(
                        text = item.uppercase(),
                        modifier = Modifier
                            .weight(weights[id]),
                        style = TextStyle(textAlign = TextAlign.Center)
                    )
                }
            }
            LazyColumn(Modifier.fillMaxSize()) {

                item {
                    if (customers.isEmpty()) {
                        Column(
                            Modifier
                                .fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null)
                            Text("Không có khách hàng")
                        }
                    }
                }

                items(customers.size) { index: Int ->
                    val customer = customers[index]
                    var selected by remember {
                        mutableStateOf(false)
                    }
                    HorizontalDivider()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "$index. ${customer.name}", modifier = Modifier
                                .weight(weights[0]),
                            style = TextStyle(textAlign = TextAlign.Center)
                        )

                        Text(
                            customer.phoneNumber, modifier = Modifier
                                .weight(weights[1]),
                            style = TextStyle(textAlign = TextAlign.Center)
                        )

                        Checkbox(
                            selected || selectAll, onCheckedChange = {
                                selected = it
                            }, modifier = Modifier
                                .weight(weights[2])
                        )

                        IconButton(
                            onClick = {}, modifier = Modifier
                                .weight(weights[3])
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}
