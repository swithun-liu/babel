package com.example.myapplication.viewmodel

import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.io.File

class FileManagerViewModel: ViewModel() {
    val fileBasePath: String = Environment.getExternalStorageDirectory().absolutePath
    var pathList: List<PathItem> by mutableStateOf(mutableListOf())

    init {
        val basePath = File(fileBasePath)
        pathList = basePath.listFiles().map {
            when {
                it.isFile -> PathItem.FileItem(
                    it.path
                )
                else -> PathItem.FolderItem(
                    it.path,
                    emptyList()
                )
            }
        }
    }
}

sealed class PathItem(val path: String) {
    val name = path
    class FolderItem(path: String, val children: List<PathItem>) : PathItem(path)
    class FileItem(path: String) : PathItem(path)
}