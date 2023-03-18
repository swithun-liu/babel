package com.example.myapplication.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.SwithunLog
import com.example.myapplication.model.ActivityVar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ServerSettingPage(activityVar: ActivityVar) {
    Row(modifier = Modifier.fillMaxHeight()) {
        Column {
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

        Box(
            modifier = Modifier
                .width(10.dp)
                .fillMaxHeight()
                .padding(2.dp)
                .background(Color.Black)
        )
        Button(onClick = {
            activityVar.nasVM.startMeAsServer()
        }) {
            Text(text = activityVar.nasVM.startMeAsServerBtnText)
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