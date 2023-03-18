package com.example.myapplication.ui.view

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.model.ActivityVar
import com.example.myapplication.model.TransferData
import kotlinx.coroutines.launch

@Composable
fun TransferPage(activityVar: ActivityVar) {
    // WordsScreen(activityVar)

    Row(
        modifier = Modifier
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(500.dp)
        ) {
            val context = LocalContext.current
            var clipboardContent: String by remember { mutableStateOf("") }

            Button(onClick = {
                val clipboard =
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val text =
                    clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: "Clipboard is empty"
                clipboardContent = text
            }) {
                Text(text = "获取剪切板内容")
            }

            BasicTextField(value =  clipboardContent, onValueChange = { clipboardContent = it })

            Button(onClick = {
                val suc = activityVar.connectServerVM.transferData(clipboardContent)
                activityVar.activity.lifecycleScope.launch {
                    // swithun-xxx todo
//                    activityVar.scaffoldState?.snackbarHostState?.showSnackbar(
//                        message = if (suc) { "成功发送" } else { "发送失败" }
//                    )
                }
            }) {
                Text(text = "发送")
            }
        }
        LazyColumn {
            items(activityVar.connectServerVM.receivedData) { data ->
                when (data) {
                    is TransferData.TextData -> {
                        TransferText(data.text)
                    }
                }
            }
        }
    }
}

@Composable
fun TransferText(text: String) {
    Text(text = text)
}
