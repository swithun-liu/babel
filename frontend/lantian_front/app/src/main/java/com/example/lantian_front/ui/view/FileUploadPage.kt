package com.example.lantian_front.ui.view

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lantian_front.Config
import com.example.lantian_front.viewmodel.connectserver.ConnectServerViewModel
import com.example.lantian_front.SwithunLog
import com.example.lantian_front.viewmodel.filemanager.FileManagerViewModel
import com.example.lantian_front.viewmodel.NasViewModel
import com.example.lantian_front.viewmodel.filemanager.Action
import com.example.lantian_front.viewmodel.filemanager.PathItem

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun FileUploadPage(
    fileManagerViewModel: FileManagerViewModel = viewModel(),
    connectServerViewModel: ConnectServerViewModel = viewModel(),
    nasViewModel: NasViewModel = viewModel(),
) {
    if (false) {
        val openDialog = remember { mutableStateOf(false) }

        val actChooseRootDir = {
            //  弹Dialog：显示文件列表(使用[FileManagerView])
            openDialog.value = true
        }
        val actFileClick = { it: PathItem.FileItem ->
            fileManagerViewModel.reduce(Action.ClickFile(it))
        }
        val actFolderClick = { it: PathItem.FolderItem ->
            fileManagerViewModel.reduce(Action.ClickFolder(it))
        }

        val funGeneratePathMoreAction: (PathItem) -> List<Pair<Pair<String, ImageVector>, (pathItem: PathItem) -> Unit>> =
            { pathItem ->
                when (pathItem) {
                    is PathItem.FileItem -> {
                        listOf(
                            "选择" to Icons.Outlined.Edit to {
                                nasViewModel.reduce(
                                    NasViewModel.Action.ChooseUploadFileRootDir(
                                        it.path
                                    )
                                )
                            },
                        )
                    }
                    is PathItem.FolderItem -> emptyList()
                }

            }

        if (openDialog.value) {
            Dialog(onDismissRequest = { openDialog.value = false }) {
                FileManagerView(
                    funGeneratePathMoreAction,
                    actFileClick,
                    actFolderClick,
                    fileManagerViewModel.uiState.uploadRootPathList
                )
            }
        }

        RootDirNotSelectedPage(actChooseRootDir)
    }

    val actFileClick = { it: PathItem.FileItem ->
        fileManagerViewModel.reduce(Action.ClickFile(it))
    }
    val actFolderClick = { it: PathItem.FolderItem ->
        fileManagerViewModel.reduce(Action.ClickFolder(it))
    }

    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = {
            SwithunLog.d(it, "file");
            it?.let { uri ->
                val appExternalPath = Config.pathConfig.appExternalPath
                val postUploadFileCachePath =  Config.pathConfig.postUploadFileCachePath

                connectServerViewModel.reduce(
                    ConnectServerViewModel.Action.PostSessionFile(
                        uri,
                        context,
                        "$appExternalPath$postUploadFileCachePath"
                    )
                )
            }
        })

    val actChooseFile = {
        launcher.launch("*/*")
    }

    RootDirSelectedPage(
        actFileClick,
        actFolderClick,
        actChooseFile,
        fileManagerViewModel.uiState.uploadRootPathList
    )
}

@RequiresApi(Build.VERSION_CODES.M)
@Preview(widthDp = 1000, heightDp = 500)
@Composable
fun RootDirSelectedPage(
    actFileClick: (file: PathItem.FileItem) -> Unit = {},
    actFolderClick: (folder: PathItem.FolderItem) -> Unit = {},
    actChooseFile: () -> Unit = {},
    dataPathItemList: List<PathItem> = listOf(
        PathItem.FolderItem("hahah", listOf(PathItem.FileItem("1"), PathItem.FileItem("1"))),
        PathItem.FileItem("1"),
        PathItem.FileItem("2"),
    ),
) {
    // 圆角矩形
    Row {
        Surface(
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .width(200.dp)
                .fillMaxHeight()
                .padding(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceTint)
        ) {
            // 垂直布局
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 按钮
                Button(
                    onClick = actChooseFile,
                    modifier = Modifier
                        .wrapContentWidth()
                        .height(48.dp)
                ) {
                    Text(text = "上传")
                }
            }
        }

        val funGeneratePathMoreAction: (PathItem) -> List<Pair<Pair<String, ImageVector>, (pathItem: PathItem) -> Unit>> =
            { emptyList() }

        Surface(
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxHeight()
                .padding(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceTint)
        ) {
            FileManagerView(
                funGeneratePathMoreAction,
                actFileClick,
                actFolderClick,
                dataPathItemList,
            )
        }
    }
}

// 根目录未选择提示界面， 提示"请选择根目录"，一个按钮点击后进入根目录选择界面
@Preview(widthDp = 1000, heightDp = 500)
@Composable
fun RootDirNotSelectedPage(
    actChooseRootDir: () -> Unit = { },
) {
    // 圆角矩形
    Surface(
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        // 垂直布局
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 提示文字
            Text(
                text = "请选择根目录⬇️",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            // 按钮
            Button(
                onClick = actChooseRootDir,
                modifier = Modifier
                    .wrapContentWidth()
                    .height(48.dp)
            ) {
                Text(text = "选择根目录")
            }
        }
    }
}
