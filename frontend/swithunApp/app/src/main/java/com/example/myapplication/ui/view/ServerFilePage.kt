package com.example.myapplication.ui.view

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.example.myapplication.*
import com.example.myapplication.model.ActivityVar
import com.example.myapplication.viewmodel.FileManagerViewModel
import com.example.myapplication.viewmodel.PathItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun ServerFilePage(activityVar: ActivityVar) {
    Row {
        Button(onClick = {
            activityVar.fileVM.viewModelScope.launch(Dispatchers.IO) {
                activityVar.fileVM.refreshBasePathListFromRemote()
            }
        }) {
            Text(text = "获取服务器文件列表")
        }
        FileManagerView(activityVar)
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun FileManagerView(activityVar: ActivityVar) {
    LazyColumn(
        modifier = Modifier
            .width(Dp(600f))
    ) {
        items(activityVar.fileVM.pathList) { path: PathItem ->
            when (path) {
                is PathItem.FileItem -> {
                    FileItemView(file = path) {
                        activityVar.fileVM.reduce(FileManagerViewModel.Action.ClickFile(it))
                    }
                }
                is PathItem.FolderItem -> {
                    FolderItemView(
                        path,
                        { activityVar.fileVM.reduce(FileManagerViewModel.Action.ClickFile(it)) },
                        { activityVar.fileVM.reduce(FileManagerViewModel.Action.ClickFolder(it)) }
                    )
                }
            }
        }
    }
}


@Composable
fun FileItemView(
    file: PathItem.FileItem = PathItem.FileItem("hahahah"),
    itemCL: (file: PathItem.FileItem) -> Unit = {},
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentHeight()
            .clickable {
                SwithunLog.d("click file: ${file.name}")
                itemCL(file)
            },
        shape = RoundedCornerShape(10),
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceTint)
    ) {
        Row(Modifier.padding(10.dp)) {
            Text(text = "File: ")
            Text(text = file.name)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@Preview(widthDp = 200, heightDp = 100)
@Composable
fun FolderItemView(
    folder: PathItem.FolderItem = PathItem.FolderItem("haha", emptyList()),
    fileItemCL: (PathItem.FileItem) -> Unit = {},
    folderItemCL: (folder: PathItem.FolderItem) -> Unit = {},
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentHeight()
            .clickable {
                SwithunLog.d("click folder: ${folder.name}")
                folderItemCL(folder)
            },
        shape = RoundedCornerShape(10),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceTint)
    ) {
        Column(
            Modifier.padding(10.dp)
        ) {
            Row {
                Text(text = "Folder: ")
                Text(text = folder.name)
            }
            if (folder.isOpening) {
                SimplePathListView(folder.children, fileItemCL, folderItemCL)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
@Preview(widthDp = 200, heightDp = 100)
fun SimplePathListView(
    pathList: List<PathItem> = listOf(
        PathItem.FolderItem("haha", emptyList()),
        PathItem.FileItem("sdfsdf")
    ),
    fileItemCL: (PathItem.FileItem) -> Unit = {},
    folderItemCL: (folder: PathItem.FolderItem) -> Unit = {},
) {
    pathList.forEach { path ->
        when (path) {
            is PathItem.FileItem -> {
                FileItemView(path, fileItemCL)
            }
            is PathItem.FolderItem -> {
                FolderItemView(path, fileItemCL, folderItemCL)
            }
        }
    }
}
