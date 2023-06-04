package com.example.myapplication.ui.view

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.Config
import com.example.myapplication.viewmodel.ConnectServerViewModel
import com.example.myapplication.SwithunLog
import com.example.myapplication.model.MessageTextDTO
import com.example.myapplication.model.TransferData
import com.example.myapplication.util.SystemUtil
import com.example.myapplication.viewmodel.NasViewModel


@Preview(widthDp = 1000, heightDp = 500)
@Composable
fun TransferPagePreviewer(
    onSendInputText: () -> Unit = {},
    receivedDataList: List<TransferData> = mutableListOf(TransferData.TextData("haha")),
    clipboardContent: String = "clipboa",
    onGetClipboardData: () -> Unit = { },
    onInputTextChanged: (String) -> Unit = { },
    onChooseFile: () -> Unit = { },
    onDataClick: (text: String, contentType: MessageTextDTO.ContentType) -> Unit = { _, _ -> },
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
            Button(onClick = onGetClipboardData) { Text(text = "获取剪切板内容") }
            BasicTextField(
                value = clipboardContent,
                onValueChange = onInputTextChanged,
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
            Button(onClick = onSendInputText) { Text(text = "发送") }
            Button(onClick = onChooseFile) {
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
            ReceivedList(receivedDataList, onDataClick)
        }
    }
}

@Composable
fun ReceivedList(
    receivedData: List<TransferData> = mutableListOf(
        TransferData.TextData("haha"),
        TransferData.ImageData("image name")
    ),
    onDataClick: (text: String, contentType: MessageTextDTO.ContentType) -> Unit = { _, _ -> },
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
    ) {
        items(receivedData) { data ->
            when (data) {
                is TransferData.TextData -> {
                    TransferText(data.text, MessageTextDTO.ContentType.TEXT, onDataClick)
                }
                is TransferData.ImageData -> {
                    TransferText(data.imageName, MessageTextDTO.ContentType.IMAGE, onDataClick)
                }
            }
        }
    }
}

@Composable
fun TransferPage(
    connectServerViewModel: ConnectServerViewModel = viewModel(),
    nasViewModel: NasViewModel = viewModel()
) {

    val context = LocalContext.current
    var clipboardContent: String by remember { mutableStateOf("") }

    val onSendInputText: () -> Unit = {
        connectServerViewModel.reduce(ConnectServerViewModel.Action.PostSessionText(clipboardContent))
    }
    val onGetClipboardContent: () -> Unit = {
        clipboardContent = SystemUtil.getTextFromClipboard(context)
    }
    val onInputTextChange = { newText: String ->
        clipboardContent = newText
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = {
            SwithunLog.d(it, "file");
            it?.let { uri ->
                val appExternalPath = Config.pathConfig.appExternalPath
                val postFileServerCachePath = Config.pathConfig.postFileServerCachePath

                connectServerViewModel.reduce(
                    ConnectServerViewModel.Action.PostSessionFile(
                        uri,
                        context,
                        "$appExternalPath$postFileServerCachePath"
                    )
                )
            }
        })

    val onChooseFile = {
        launcher.launch("*/*")
    }

    val onReceivedDataClick: (text: String, contentType: MessageTextDTO.ContentType) -> Unit =
        { text, contentType ->
            nasViewModel.reduce(
                NasViewModel.Action.DownloadTransferFile(text, contentType, context)
            )
        }

    TransferPagePreviewer(
        onSendInputText,
        connectServerViewModel.receivedData,
        clipboardContent,
        onGetClipboardContent,
        onInputTextChange,
        onChooseFile,
        onReceivedDataClick
    )
}

@Preview
@Composable
fun TransferText(
    text: String = "haha是的发送到发送到发送到发送sdfssdfsdf哈哈hahasdf是的发送到发送地方",
    contentType: MessageTextDTO.ContentType = MessageTextDTO.ContentType.IMAGE,
    onDataClick: (text: String, contentType: MessageTextDTO.ContentType) -> Unit = { _, _ -> },
) {
    Surface(
        modifier = Modifier
            .padding(5.dp),
        color = MaterialTheme.colorScheme.surfaceTint,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = text, modifier = Modifier.padding(5.dp))
            }
            Box(modifier = Modifier.width(50.dp)) {
                when (contentType) {
                    MessageTextDTO.ContentType.TEXT ->
                        Icon(Icons.Outlined.AddCircle,
                            contentDescription = "Download",
                            modifier = Modifier
                                .size(50.dp)
                                .clickable {
                                })
                    MessageTextDTO.ContentType.IMAGE ->
                        Icon(Icons.Outlined.Email,
                            contentDescription = "Download",
                            modifier = Modifier
                                .size(50.dp)
                                .clickable {
                                    onDataClick(text, contentType)
                                })
                }
            }
        }
    }
}
