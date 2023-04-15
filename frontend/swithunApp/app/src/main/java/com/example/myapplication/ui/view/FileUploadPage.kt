package com.example.myapplication.ui.view

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.myapplication.model.ActivityVar
import com.example.myapplication.viewmodel.FileManagerViewModel
import com.example.myapplication.viewmodel.NasViewModel
import com.example.myapplication.viewmodel.PathItem

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun FileUploadPage(activityVar: ActivityVar) {
    val openDialog = remember { mutableStateOf(false) }

    val actChooseRootDir = {
        //  弹Dialog：显示文件列表(使用[FileManagerView])
        openDialog.value = true
    }
    val actFileClick = { it: PathItem.FileItem ->
        activityVar.fileVM.reduce(FileManagerViewModel.Action.ClickFile(it))
    }
    val actFolderClick = { it: PathItem.FolderItem ->
        activityVar.fileVM.reduce(FileManagerViewModel.Action.ClickFolder(it))
    }

    if (openDialog.value) {
        Dialog(onDismissRequest = { openDialog.value = false }) {
            FileManagerView(
                actFileClick,
                actFolderClick,
                activityVar.fileVM.pathList
            )
        }
    }


//        activityVar.nasVM.reduce(NasViewModel.Action.ChooseUploadFileRootDir)
    RootDirNotSelectedPage(actChooseRootDir)
}

// 根目录未选择提示界面， 提示"请选择根目录"，一个按钮点击后进入根目录选择界面
@Preview(widthDp = 1000, heightDp = 500)
@Composable
fun RootDirNotSelectedPage(
    actChooseRootDir: () -> Unit = { }
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
