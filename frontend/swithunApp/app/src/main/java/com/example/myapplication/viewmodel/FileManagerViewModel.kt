package com.example.myapplication.viewmodel

import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.myapplication.nullCheck
import java.io.File

class FileManagerViewModel: ViewModel() {

    val fileBasePath: String = Environment.getExternalStorageDirectory().absolutePath
    var pathList: List<PathItem> by mutableStateOf(mutableListOf())

    init {
        val basePath = File(fileBasePath)
        pathList = basePath.listFiles()?.map2Items() ?: emptyList()
    }

    fun clickFolder(folder: PathItem.FolderItem) {
        when {
            // 【折叠动作】
            folder.isOpening -> {
                folder.isOpening = false
            }
            // 【展开动作】
            else -> {
                folder.isOpening = true
                val parent = File(folder.path)
                folder.children = parent.listFiles()?.map2Items().nullCheck("children: ") ?: emptyList()
            }
        }
        pathList = pathList.toList()
    }


    private fun Array<File>.map2Items(): List<PathItem> {
        return this.map {
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
    class FolderItem(path: String, var children: List<PathItem>) : PathItem(path) {
        var isOpening = false
    }
    class FileItem(path: String) : PathItem(path)
}