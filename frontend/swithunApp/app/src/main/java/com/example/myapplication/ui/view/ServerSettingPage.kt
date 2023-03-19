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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.SwithunLog
import com.example.myapplication.model.ActivityVar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Preview
@Composable
fun Text() {
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .padding(0.dp, 10.dp, 5.dp, 10.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = "haha")
            Row {
                Button(onClick = {
                }) {
                    Text(text = "haha")
                }
            }
        }
    }
}


@Composable
fun ServerSettingPage(activityVar: ActivityVar) {
    Row(modifier = Modifier
        .fillMaxHeight()
        .fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .padding(0.dp, 10.dp, 5.dp, 10.dp),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(text = activityVar.ftpVM.myIPStr)
                Row {
                    Button(onClick = {
                        activityVar.activity.lifecycle
                        activityVar.activity.lifecycleScope.launch(Dispatchers.IO) {
                            SwithunLog.d("begin get ips")
                            val ips = activityVar.nasVM.searchAllServer()
                            SwithunLog.d(ips)
                        }
                    }) {
                        Text(text = activityVar.nasVM.getAllServerBtnText)
                    }
                    ServerList(activityVar)
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .padding(5.dp, 10.dp, 10.dp, 10.dp),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Button(onClick = {
                    activityVar.nasVM.startMeAsServer()
                }) {
                    Text(text = activityVar.nasVM.startMeAsServerBtnText)
                }
            }
        }

    }
}

@Composable
fun ServerList(activityVar: ActivityVar) {
    LazyColumn(modifier = Modifier.width(Dp(200f))) {
        items(activityVar.nasVM.allServersInLan) { serverIp: String ->
            Button(onClick = {
                activityVar.connectServerVM.connectServer(serverIp)
            }) {
                Text(text = "连接 $serverIp")
            }
        }
    }
}