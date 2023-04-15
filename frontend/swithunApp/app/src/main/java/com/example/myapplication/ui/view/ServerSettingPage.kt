package com.example.myapplication.ui.view

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.ConnectServerViewModel
import com.example.myapplication.SwithunLog
import com.example.myapplication.model.ActivityVar
import com.example.myapplication.viewmodel.NasViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ServerSettingPage(activityVar: ActivityVar) {
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
    ) {

        val actSearchServerInLan: () -> Unit = {
            activityVar.activity.lifecycle
            activityVar.activity.lifecycleScope.launch(Dispatchers.IO) {
                SwithunLog.d("begin get ips")
                activityVar.nasVM.reduce(NasViewModel.Action.SearchAllServer)
            }
        }

        val actConnectSearchedServerIp = { serverIp: String ->
            activityVar.connectServerVM.reduce(
                ConnectServerViewModel.Action.ConnectServer(
                    serverIp
                )
            )
        }

        val actChangeLastTimeServerIp = { newSeverIp: String ->
            activityVar.nasVM.lastTimeConnectServerIp = newSeverIp
        }

        val actConnectLastTimeServer = {
            activityVar.connectServerVM.reduce(
                ConnectServerViewModel.Action.ConnectServer(
                    activityVar.nasVM.lastTimeConnectServerIp
                )
            )
        }

        ServerOther(
            activityVar.kernelConfig.kernelIP /* 当前IP */,
            activityVar.nasVM.allServersInLan /* 所有服务器IP */,
            activityVar.nasVM.lastTimeConnectServerIp /* 上次连接的服务器IP */,
            activityVar.nasVM.getAllServerBtnText /* 搜索服务器按钮文字 */,
            actConnectSearchedServerIp /* 连接搜索到的服务器 */,
            actSearchServerInLan /* 搜索服务器 */,
            actChangeLastTimeServerIp /* 修改上次连接的服务器IP */,
            actConnectLastTimeServer /* 连接上次连接的服务器 */,
        )

        val actStartServer = { activityVar.nasVM.reduce(NasViewModel.Action.StartMeAsServer) }
        val actConnectMyServer = { activityVar.nasVM.reduce(NasViewModel.Action.ConnectMyServer) }

        ServerMine(
            actStartServer,
            actConnectMyServer,
            activityVar.nasVM.startMeAsServerBtnText /* startServerBtnText */
        )
    }
}

@Preview(widthDp = 500, heightDp = 500)
@Composable
fun ServerMine(
    actStartServer: () -> Unit = {},
    actConnectMyServer: () -> Unit = { },
    startServerBtnText: String = "启动server",
) {
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(1.toFloat())
            .padding(5.dp, 10.dp, 10.dp, 10.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Button(onClick = actStartServer) {
                Text(text = startServerBtnText)
            }
            Button(onClick = actConnectMyServer) {
                Text(text = "连接我的server")
            }
        }
    }
}

@Preview(widthDp = 500, heightDp = 500)
@Composable
fun ServerOther(
    myIp: String = "12.234.23.23",
    data4AllServersInLan: List<String> = listOf("sdfsdf", "sdfsdf"),
    text4LastTimeServerIp: String = "12.234.23.23",
    text4SearchBtn: String = "bbbbb",
    actConnectSearchedServerIp: (ip: String) -> Unit = {},
    actSearchServerInLan: () -> Unit = {},
    actChangeLastTimeServerIp: (newIp: String) -> Unit = {},
    actConnectLastTimeServer: () -> Unit = { },
) {
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(1.toFloat())
            .padding(0.dp, 10.dp, 5.dp, 10.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = "设备IP: $myIp")
            Surface(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
            ) {
                Row(
                    modifier = Modifier.padding(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        modifier = Modifier.wrapContentSize(),
                        shape = RoundedCornerShape(10.dp),
                        onClick = actConnectLastTimeServer
                    ) {
                        Text(text = "连接上次 ->")
                    }

                    // 垂直居中
                    BasicTextField(
                        value = text4LastTimeServerIp,
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
                            actChangeLastTimeServerIp.invoke(it)
                        },
                    )
                }
            }
            Row {
                Button(onClick = actSearchServerInLan) {
                    Text(text = text4SearchBtn)
                }
                LazyColumn(modifier = Modifier.width(Dp(200f))) {
                    items(data4AllServersInLan) { serverIp: String ->
                        Button(onClick = {
                            actConnectSearchedServerIp(serverIp)
                        }) {
                            Text(text = "连接 $serverIp")
                        }
                    }
                }
            }
        }
    }
}