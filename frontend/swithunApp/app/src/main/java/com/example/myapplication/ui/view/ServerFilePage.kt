package com.example.myapplication.ui.view

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.example.myapplication.*
import com.example.myapplication.R
import com.example.myapplication.viewmodel.PathItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun ServerFilePage(activityVar: ActivityVar) {
    Row {
        Button(onClick = {
            activityVar.fileManagerViewModel.viewModelScope.launch(Dispatchers.IO) {
                activityVar.fileManagerViewModel.refreshBasePathListFromRemote()
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
            .background(Color(R.color.purple_200))
            .width(Dp(600f))
    ) {
        items(activityVar.fileManagerViewModel.pathList) { path: PathItem ->
            when (path) {
                is PathItem.FileItem -> {
                    FileItemView(path, activityVar)
                }
                is PathItem.FolderItem -> {
                    FolderItemView(path, activityVar)
                }
            }
        }
    }
}


@Composable
fun FileItemView(file: PathItem.FileItem, activityVar: ActivityVar) {
    Card(modifier = Modifier
        .fillMaxSize()
        .clickable {
            SwithunLog.d("click file: ${file.name}")
            activityVar.fileManagerViewModel.clickFile(file)
        }
    ) {
        Row(Modifier.padding(10.dp)) {
            Text(text = "File: ")
            Text(text = file.name)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun FolderItemView(folder: PathItem.FolderItem, activityVar: ActivityVar) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                SwithunLog.d("click folder: ${folder.name}")
                activityVar.fileManagerViewModel.viewModelScope.launch(Dispatchers.IO) {
                    activityVar.fileManagerViewModel.clickFolder(folder)
                }
            },
        backgroundColor = Color(activityVar.activity.getColor(R.color.teal_200))
    ) {
        Column(
            Modifier.padding(10.dp)
        ) {
            Row {
                Text(text = "Folder: ")
                Text(text = folder.name)
            }
            if (folder.isOpening) {
                SimplePathListView(folder.children, activityVar)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun SimplePathListView(pathList: List<PathItem>, activityVar: ActivityVar) {
    pathList.forEach { path ->
        when (path) {
            is PathItem.FileItem -> {
                FileItemView(path, activityVar)
            }
            is PathItem.FolderItem -> {
                FolderItemView(path, activityVar)
            }
        }
    }
}
