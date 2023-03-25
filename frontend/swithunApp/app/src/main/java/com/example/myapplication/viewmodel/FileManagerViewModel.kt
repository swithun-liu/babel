package com.example.myapplication.viewmodel

import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.myapplication.model.ActivityVar
import com.example.myapplication.SwithunLog
import com.example.myapplication.model.ServerConfig
import com.example.myapplication.model.VideoExtension
import java.io.File

class FileManagerViewModel : ViewModel() {

    val fileBasePath: String = Environment.getExternalStorageDirectory().absolutePath
    var pathList: List<PathItem> by mutableStateOf(listOf())
    var activityVar: ActivityVar? = null

    private val remoteRepository = FileManagerHTTPRepository { activityVar }
    private val localRepository = FileManagerLocalRepository(fileBasePath)

    fun init(activityVar: ActivityVar) {
        this.activityVar = activityVar
    }

    suspend fun clickFolder(folder: PathItem.FolderItem) {
        when {
            // 【折叠动作】
            folder.isOpening -> {
                folder.isOpening = false
            }
            // 【展开动作】
            else -> {
                folder.isOpening = true
                refreshChildrenPathListFromRemote(folder)
            }
        }
        // todo 看看怎么做不用这样赋值
        val oldList = pathList
        pathList = mutableListOf()
        pathList = oldList
    }

    fun clickFile(file: PathItem.FileItem) {
        activityVar?.let {
            val fileObj = File(file.path)
            if (VideoExtension.isOneOf(fileObj.extension)) {
                val surfaceView = it.mySurfaceView ?: return
                it.videoVM.play(
                    "http://${ServerConfig.serverHost}/${ServerConfig.ServerPath.GetVideoPath.path}?${ServerConfig.ServerPath.GetVideoPath.paramPath}=${file.path}"
                )
            } else {
                SwithunLog.e("不是视频")
            }
        }
    }

    private suspend fun refreshChildrenPathListFromRemote(folder: PathItem.FolderItem) {
        folder.children = getChildrenPathListFromRemote(folder.path)
    }

    fun getBasePathListFromLocal(): List<LocalPathItem> {
        return localRepository.getBasePathList()
    }

    fun getChildrenPathListFromLocal(parentPath: String): List<LocalPathItem> {
        return localRepository.getChildrenPathList(parentPath)
    }

    suspend fun refreshBasePathListFromRemote() {
        pathList = remoteRepository.getBasePathList()
    }

    suspend fun getChildrenPathListFromRemote(parentPath: String): List<PathItem> {
        return remoteRepository.getChildrenPathList(parentPath)
    }

}

sealed class PathItem(val path: String) {
    val name = path

    class FolderItem(path: String, var children: List<PathItem>) : PathItem(path) {
        var isOpening = false
    }

    class FileItem(path: String) : PathItem(path)
}

data class LocalPathItem(val path: String, val fileType: Int) {

    enum class PathType(val type: Int) {
        FILE(0),
        Folder(1);

        companion object {
            fun fromValue(value: Int): PathType? = values().find { it.type == value }
        }

    }

}