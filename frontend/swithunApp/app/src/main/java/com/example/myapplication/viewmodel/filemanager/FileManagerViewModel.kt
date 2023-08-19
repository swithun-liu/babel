package com.example.myapplication.viewmodel.filemanager

import android.os.Environment
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.myapplication.Config
import com.example.myapplication.model.VMCollection
import com.example.myapplication.SwithunLog
import com.example.myapplication.framework.BaseViewModel
import com.example.myapplication.model.ServerConfig
import com.example.myapplication.model.VideoExtension
import com.example.myapplication.viewmodel.ConnectKernelViewModel
import com.example.myapplication.viewmodel.FileManagerHTTPRepository
import com.example.myapplication.viewmodel.FileManagerLocalRepository
import com.example.myapplication.viewmodel.VideoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.jahnen.libaums.core.fs.FileSystem
import me.jahnen.libaums.core.fs.UsbFile
import java.io.File

// Babel / 蓝田

class FileManagerViewModel : BaseViewModel<FileManagerViewModel.Action, FileManagerViewModel.UIState, FileManagerViewModel.MutableUIState>() {

    private val fileBasePath: String = Environment.getExternalStorageDirectory().absolutePath
    private var vmCollection: VMCollection? = null
    private val remoteRepository = FileManagerHTTPRepository()
    private val localRepository = FileManagerLocalRepository(fileBasePath)
    private var usbRoot: UsbFile? = null

    @Stable
    interface UIState {
        var pathList: List<PathItem>
        var uploadRootPathList: List<PathItem>
    }

    class MutableUIState: UIState {
        override var pathList: List<PathItem> by mutableStateOf(listOf())
        override var uploadRootPathList: List<PathItem> by mutableStateOf(listOf())
    }

    override fun getInitialUIState(): MutableUIState {
        return MutableUIState()
    }

    fun init(vmCollection: VMCollection) {
        this.vmCollection = vmCollection
        SwithunLog.d("fileBasePath: $fileBasePath")

        val appExternalPath = Config.pathConfig.appExternalPath

        uiState.uploadRootPathList = listOf(
            PathItem.FolderItem(appExternalPath, emptyList())
        )
    }

    fun initUsbDevices(currentFs: FileSystem) {

        try {
            SwithunLog.d("usb Capacity: " + currentFs.capacity)
            SwithunLog.d("usb Occupied Space: " + currentFs.occupiedSpace)
            SwithunLog.d("usb 3")
            SwithunLog.d("usb Free Space: " + currentFs.freeSpace)
            SwithunLog.d("usb Chunk size: " + currentFs.chunkSize)
            SwithunLog.d("usb 4")
            val root: UsbFile = currentFs.rootDirectory
            this.usbRoot = root
            SwithunLog.d("usb 5")
            val files: Array<UsbFile> = root.listFiles()
            for (file in files) {
                SwithunLog.d("usb file: " + file.name)
            }
        } catch (e: java.lang.Exception) {
            SwithunLog.d("vm usb exception: $e")
        }
    }

    private fun findUsbFile(action: Action.FindUsbFile){
        val file = run {
            val usbRoot = this.usbRoot ?: return@run null

            val files: Array<UsbFile> = usbRoot.listFiles()
            for (file in files) {
                SwithunLog.d("usb file: " + file.name)
                if (file.name == action.path) {
                    return@run file
                }
            }
            return@run null
        }

        vmCollection?.connectKernelVM?.reduce(ConnectKernelViewModel.Action.ServerGetAndroidUsbFileFileManagerResponse(file, action.uuid))
    }

    sealed class Action: BaseViewModel.Action() {
        class ClickFolder(val folder: PathItem.FolderItem): Action()
        class ClickFile(val file: PathItem.FileItem): Action()
        object RefreshBasePathListFromRemote: Action()
        class FindUsbFile(val path: String, val uuid: String): Action()
    }

    override fun reduce(action: Action) {
        when (action) {
            is Action.ClickFile -> clickFile(action.file)
            is Action.ClickFolder -> clickFolder(action.folder)
            Action.RefreshBasePathListFromRemote -> refreshBasePathListFromRemote()
            is Action.FindUsbFile -> findUsbFile(action)
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
            val oldList = innerUISate.pathList
            innerUISate.pathList = mutableListOf()
            innerUISate.pathList = addMainToFirst(oldList.toMutableList())

            val oldUploadList = uiState.uploadRootPathList
            uiState.uploadRootPathList = mutableListOf()
            uiState.uploadRootPathList = oldUploadList.toMutableList()
        }
    }

    private fun clickFile(file: PathItem.FileItem) {
        vmCollection?.let {
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
            uiState.pathList = addMainToFirst(remoteRepository.getBasePathList().toMutableList())
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