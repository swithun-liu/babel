package com.example.lantian_front.ui.view

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lantian_front.ui.view.component.SCommonButton
import com.example.lantian_front.viewmodel.serversetting.Action
import com.example.lantian_front.viewmodel.serversetting.ServerSettingViewModel

@Preview(widthDp = 1000, heightDp = 500)
@Composable
fun ServerSettingPage2(
    viewModel: ServerSettingViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
) {
    val onSearchServerClick = { lanIp: String -> viewModel.reduce(Action.SearchServer(lanIp)) }
    val searchServerBtnText = viewModel.uiState.searchServerBtnText
    val availableServerIPs = viewModel.uiState.availableServerIPs
    val connectServer = { serverIp: String -> viewModel.reduce(Action.ConnectServer(serverIp)) }
    val lastTimeConnectServerIp = viewModel.uiState.lastTimeConnectServerIp

    Page(
        onSearchServerClick,
        searchServerBtnText,
        availableServerIPs,
        connectServer,
        lastTimeConnectServerIp
    )
}

@Preview(widthDp = 1000, heightDp = 500)
@Composable
fun Page(
    onSearchServerClick: (lanIp: String) -> Unit = { },
    searchServerBtnText: String = "search again",
    availableServerIPs: List<String> = listOf("server ip1", "server ip1"),
    connectServer: (ip: String) -> Unit = { },
    lastTimeConnectServerIp: String = "last time ip",
) {
    Row {

        SearchInLan(
            onSearchServerClick,
            searchServerBtnText,
            lastTimeConnectServerIp
        )

        Column {
            ConnectLastTime(
                connectServer,
                lastTimeConnectServerIp
            )
            ConnectManual(
                connectServer,
                lastTimeConnectServerIp
            )
            AvailableServerList(
                availableServerIPs,
                connectServer
            )
        }
    }
}

@Composable
private fun SearchInLan(
    onSearchServerClick: (lanIp: String) -> Unit = { },
    searchServerBtnText: String = "search again",
    lastTimeConnectServerIp: String
) {

    var lanIp by remember {
        val items = lastTimeConnectServerIp.split(".").toMutableList()
        val preIp = if (items.size == 4) {
            items.removeAt(3)
            items.joinToString(".")
        } else {
            "192.168.0"
        }

        mutableStateOf(preIp)
    }


    Row(
        modifier = Modifier.padding(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 垂直居中
        BasicTextField(
            value = lanIp,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(size = 16.dp)
                        )
                        .padding(10.dp)
                ) {
                    innerTextField()
                }
            },
            onValueChange = {
                lanIp = it
            },
        )

        SCommonButton(onClick = { onSearchServerClick.invoke(lanIp) } ) {
            Text(text = searchServerBtnText)
        }

    }
}

@Composable
private fun ConnectLastTime(
    connectServer: (ip: String) -> Unit = { },
    lastTimeConnectServerIp: String = "last time ip",
) {
    Text(text = "上次连接")
    SCommonButton(onClick = { connectServer.invoke(lastTimeConnectServerIp) }) {
        Text(text = lastTimeConnectServerIp)
    }
}

@Composable
private fun ConnectManual(
    connectServer: (ip: String) -> Unit = { },
    lastTimeConnectServerIp: String = "last time ip",
) {

    var manualFillServerIp by remember { mutableStateOf(lastTimeConnectServerIp) }


    Text(text = "手动连接")

    Row(
        modifier = Modifier.padding(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 垂直居中
        BasicTextField(
            value = manualFillServerIp,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(size = 16.dp)
                        )
                        .padding(10.dp)
                ) {
                    innerTextField()
                }
            },
            onValueChange = {
                manualFillServerIp = it
            },
        )

        SCommonButton(onClick = { connectServer.invoke(manualFillServerIp) } ) {
            Text(text = "-> 连接")
        }

    }
}

@Composable
private fun AvailableServerList(
    availableServerIPs: List<String> = listOf("server ip1", "server ip1"),
    connectServer: (ip: String) -> Unit = { },
) {
    Text(text = "可用server")
    LazyColumn() {
        items(availableServerIPs) { ip ->
            SCommonButton(onClick = { connectServer.invoke(ip) }) {
                Text(text = ip)
            }
        }
    }
}