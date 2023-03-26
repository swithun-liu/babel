package com.example.myapplication.ui.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.SwithunLog
import com.example.myapplication.model.ActivityVar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ServerSettingPage(activityVar: ActivityVar) {
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
    ) {
        ServerOther(myIp = activityVar.ftpVM.myIPStr,
            {
                activityVar.activity.lifecycle
                activityVar.activity.lifecycleScope.launch(Dispatchers.IO) {
                    SwithunLog.d("begin get ips")
                    val ips = activityVar.nasVM.searchAllServer()
                    SwithunLog.d(ips)
                }
            },
            activityVar.nasVM.getAllServerBtnText,
            activityVar.nasVM.allServersInLan,
            { serverIp ->
                activityVar.connectServerVM.connectServer(serverIp)
            }
        )

        ServerMine(
            {
                activityVar.nasVM.startMeAsServer()
            },
            activityVar.nasVM.startMeAsServerBtnText
        )
    }
}

@Preview(widthDp = 500, heightDp = 500)
@Composable
fun ServerMine(startServerCL: () -> Unit = {}, startServerBtnText: String = "bbbb") {
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(1.toFloat())
            .padding(5.dp, 10.dp, 10.dp, 10.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Button(onClick = startServerCL) {
                Text(text = startServerBtnText)
            }
        }
    }
}

@Preview(widthDp = 500, heightDp = 500)
@Composable
fun ServerOther(
    myIp: String = "12.234.23.23",
    searchCL: () -> Unit = {},
    searchBtnText: String = "bbbbb",
    allServersInLan: List<String> = listOf("sdfsdf", "sdfsdf"),
    serverItemCL: (ip: String) -> Unit = {},
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
            Text(text = myIp)
            Row {
                Button(onClick = searchCL) {
                    Text(text = searchBtnText)
                }
                LazyColumn(modifier = Modifier.width(Dp(200f))) {
                    items(allServersInLan) { serverIp: String ->
                        Button(onClick = {
                            serverItemCL(serverIp)
                        }) {
                            Text(text = "连接 $serverIp")
                        }
                    }
                }
            }
        }
    }
}