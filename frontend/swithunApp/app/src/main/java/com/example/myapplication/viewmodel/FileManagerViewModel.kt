package com.example.myapplication.viewmodel

import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.myapplication.model.ActivityVar
import com.example.myapplication.SwithunLog
import com.example.myapplication.model.ServerConfig
import com.example.myapplication.model.VideoExtension
import com.example.myapplication.nullCheck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

// Babel / 蓝田

class FileManagerViewModel : BaseViewModel<FileManagerViewModel.Action>() {

    val fileBasePath: String = Environment.getExternalStorageDirectory().absolutePath
    var pathList: List<PathItem> by mutableStateOf(listOf())
    var uploadRootPathList: List<PathItem> by mutableStateOf(listOf())
    var activityVar: ActivityVar? = null

    private val remoteRepository = FileManagerHTTPRepository { activityVar }
    private val localRepository = FileManagerLocalRepository(fileBasePath)

    fun init(activityVar: ActivityVar) {
        this.activityVar = activityVar
        SwithunLog.d("fileBasePath: $fileBasePath")

        val appExternalPath = activityVar.pathConfig.appExternalPath.nullCheck("获取上传文件根路径", true) ?: return

        uploadRootPathList = listOf(
            PathItem.FolderItem(appExternalPath, emptyList())
        )
    }

    sealed class Action: BaseViewModel.Action() {
        class ClickFolder(val folder: PathItem.FolderItem): Action()
        class ClickFile(val file: PathItem.FileItem): Action()
        object RefreshBasePathListFromRemote: Action()
    }

    override fun reduce(action: Action) {
        when (action) {
            is Action.ClickFile -> clickFile(action.file)
            is Action.ClickFolder -> clickFolder(action.folder)
            Action.RefreshBasePathListFromRemote -> refreshBasePathListFromRemote()
        }
    }

    private fun clickFolder(folder: PathItem.FolderItem) {
        viewModelScope.launch(Dispatchers.IO) {
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
            pathList = addMainToFirst(oldList.toMutableList())

            val oldUploadList = uploadRootPathList
            uploadRootPathList = mutableListOf()
            uploadRootPathList = oldUploadList.toMutableList()
        }
    }

    private fun clickFile(file: PathItem.FileItem) {
        activityVar?.let {
            val fileObj = File(file.path)
            if (VideoExtension.isOneOf(fileObj.extension)) {
                it.videoVM.reduce(
                    VideoViewModel.Action.PlayVideoAction(
                        "http://${ServerConfig.serverHost}/${ServerConfig.ServerPath.GetVideoPath.path}?${ServerConfig.ServerPath.GetVideoPath.paramPath}=${file.path}"
                    )
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

    private fun refreshBasePathListFromRemote() {
        viewModelScope.launch(Dispatchers.IO) {
            pathList = addMainToFirst(remoteRepository.getBasePathList().toMutableList())
        }
    }

    private fun addMainToFirst(basePathList: MutableList<PathItem>): MutableList<PathItem> {
        val a = basePathList.find {
            it.path == "$fileBasePath/babel"
        }
        if (a != null) {
            basePathList.remove(a)
            basePathList.add(0, a)
        }
        return basePathList
    }

    private suspend fun getChildrenPathListFromRemote(parentPath: String): List<PathItem> {
        return remoteRepository.getChildrenPathList(parentPath)
    }

    fun getCacheTransferDataParent(): File {
        val p = "/babel/cache/transfer"
        val dir = File(fileBasePath, p)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
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