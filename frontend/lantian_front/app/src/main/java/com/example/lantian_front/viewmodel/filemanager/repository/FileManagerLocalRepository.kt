package com.example.lantian_front.viewmodel.filemanager.repository

import com.example.lantian_front.viewmodel.filemanager.model.LocalPathItem
import java.io.File

class FileManagerLocalRepository(private val fileBasePath: String) {

    fun getBasePathList(): List<LocalPathItem> {
        return getChildrenPathList(fileBasePath)
    }

    private fun Array<File>.map2LocalItems(): List<LocalPathItem> {
        return this.map {
            LocalPathItem(
                it.path,
                if (it.isFile) LocalPathItem.PathType.FILE.type else LocalPathItem.PathType.Folder.type
            )
        }
    }

    fun getChildrenPathList(parentPath: String): List<LocalPathItem> {
        val parentPath = File(parentPath)
        return parentPath.listFiles()?.map2LocalItems() ?: emptyList()
    }
}