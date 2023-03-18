package com.example.myapplication.ui.view

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Button
import androidx.compose.material.Snackbar
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.ActivityVar
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

            TextField(value = clipboardContent, onValueChange = { clipboardContent = it })

            Button(onClick = {
                val suc = activityVar.connectServerVM.transferData(clipboardContent)
                activityVar.activity.lifecycleScope.launch {
                    activityVar.scaffoldState?.snackbarHostState?.showSnackbar(
                        message = if (suc) { "成功发送" } else { "发送失败" }
                    )
                }
            }) {
                Text(text = "发送")
            }
        }
    }

}
