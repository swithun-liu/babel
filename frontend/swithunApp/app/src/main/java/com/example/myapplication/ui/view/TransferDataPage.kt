package com.example.myapplication.ui.view

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.SwithunLog
import com.example.myapplication.model.ActivityVar
import com.example.myapplication.model.TransferData
import com.example.myapplication.nullCheck
import kotlinx.coroutines.launch


@Preview(widthDp = 1000, heightDp = 500)
@Composable
fun TransferPagePreviewer(
    sendCL: () -> Unit = {},
    receivedData: List<TransferData> = mutableListOf(TransferData.TextData("haha")),
    clipboardContent: String = "clipboa",
    getClipBoardDataCL: () -> Unit = { },
    onTextValueChange: (String) -> Unit = { },
    chosenFileCL: () -> Unit = { }
) {
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(2.toFloat()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1.toFloat())
                .width(500.dp)
        ) {
            Button(onClick = getClipBoardDataCL) { Text(text = "获取剪切板内容") }
            BasicTextField(
                value = clipboardContent,
                onValueChange = onTextValueChange,
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp, vertical = 10.dp) //margin
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(size = 16.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp) // inner padding
                    ) {
                        innerTextField()
                    }
                }
            )
            Button(onClick = sendCL) { Text(text = "发送") }
            Button(onClick = chosenFileCL) {
                Text(text = "选择文件")
            }
        }
        Surface(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxHeight()
                .aspectRatio(1.toFloat()),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp)
        ) {
            ReceivedList(receivedData)
        }
    }
}

@Preview
@Composable
fun ReceivedList(
    receivedData: List<TransferData> = mutableListOf(TransferData.TextData("haha"))
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
    ) {
        items(receivedData) { data ->
            when (data) {
                is TransferData.TextData -> {
                    TransferText(data.text)
                }
            }
        }
    }
}

@Composable
fun TransferPage(activityVar: ActivityVar) {

    val context = LocalContext.current
    var clipboardContent: String by remember { mutableStateOf("") }

    val sendCL: () -> Unit = {
        val suc = activityVar.connectServerVM.transferData(clipboardContent)
        activityVar.activity.lifecycleScope.launch {
            activityVar.scaffoldState?.showSnackbar(
                message = if (suc) {
                    "成功发送"
                } else {
                    "发送失败"
                }
            )
        }
    }
    val getClipBoardDataCL: () -> Unit = {
        val clipboard =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text =
            clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: "Clipboard is empty"
        clipboardContent = text
    }
    val onTextValueChange = { newText: String ->
        clipboardContent = newText
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = {
            SwithunLog.d(it, "file");
            it?.let { uri ->
                activityVar.connectServerVM.transferData(uri, context)
            }
        })

    val chosenFileCL = {
        launcher.launch("*/*")
    }

    TransferPagePreviewer(
        sendCL,
        activityVar.connectServerVM.receivedData,
        clipboardContent,
        getClipBoardDataCL,
        onTextValueChange,
        chosenFileCL
    )
}

@Preview
@Composable
fun TransferText(text: String = "haha") {
    Surface(modifier = Modifier
        .padding(5.dp),
        color = MaterialTheme.colorScheme.surfaceTint,
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(text = text, modifier = Modifier.padding(5.dp))
    }
}
