package com.example.lantian_front.viewmodel.filemanager

import android.os.Environment
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.lantian_front.Config
import com.example.lantian_front.model.VMCollection
import com.example.lantian_front.SwithunLog
import com.example.lantian_front.framework.BaseViewModel
import com.example.lantian_front.model.FileItem
import com.example.lantian_front.model.ServerConfig
import com.example.lantian_front.model.Storage
import com.example.lantian_front.model.VideoExtension
import com.example.lantian_front.viewmodel.ConnectKernelViewModel
import com.example.lantian_front.viewmodel.filemanager.repository.FileManagerHTTPRepository
import com.example.lantian_front.viewmodel.filemanager.repository.FileManagerLocalRepository
import com.example.lantian_front.viewmodel.filemanager.repository.FileManagerRepository
import com.example.lantian_front.viewmodel.VideoViewModel
import com.example.lantian_front.viewmodel.filemanager.model.LocalPathItem
import com.example.lantian_front.viewmodel.filemanager.model.PathItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.jahnen.libaums.core.fs.FileSystem
import me.jahnen.libaums.core.fs.UsbFile
import java.io.File
import java.nio.ByteBuffer

// Babel / 蓝田

class FileManagerViewModel : BaseViewModel<Action, FileManagerViewModel.UIState, FileManagerViewModel.MutableUIState, BaseViewModel.AEvent>() {

    private val fileBasePath: String = Environment.getExternalStorageDirectory().absolutePath
    private var vmCollection: VMCollection? = null
    private val remoteRepository = FileManagerHTTPRepository()
    private val localRepository = FileManagerLocalRepository(fileBasePath)
    private val repository = FileManagerRepository(fileBasePath)
    private var usbRoot: UsbFile? = null

    @Stable
    interface UIState {
        val pathList: List<PathItem>
        val storageList: List<Storage>
        val uploadRootPathList: List<PathItem>
    }

    class MutableUIState: UIState {
        override var pathList: List<PathItem> by mutableStateOf(listOf())
        override var storageList: List<Storage> by mutableStateOf(listOf())
        override var uploadRootPathList: List<PathItem> by mutableStateOf(listOf())
    }

    override fun getInitialUIState(): MutableUIState {
        return MutableUIState()
    }

    fun init(vmCollection: VMCollection) {
        this.vmCollection = vmCollection
        SwithunLog.d("fileBasePath: $fileBasePath")

        val appExternalPath = Config.pathConfig.appExternalPath

//        innerUISate.uploadRootPathList = listOf(
//            PathItem.FolderItem(appExternalPath, emptyList())
//        )
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

    private fun findUsbFileForName(action: Action.FindUsbFile){
        val file = findUsbFile(action.path)
        SwithunLog.d(" findUsbFileForName after file")

        vmCollection?.connectKernelVM?.reduce(ConnectKernelViewModel.Action.ServerGetAndroidUsbFileFileManagerResponse(file, action.uuid))
    }

    private fun findUsbFile(path: String): UsbFile? {
        SwithunLog.d("findUsbFile path: $path")
        val usbRoot = this.usbRoot ?: return null

        val files: Array<UsbFile> = usbRoot.listFiles()

        SwithunLog.d("findUsbFile file: ${files.size}")

        for (file in files) {
            SwithunLog.d("usb file: " + file.name)
            if (file.name == path) {
                SwithunLog.d("findUsbFile return file")
                return file
            }
        }
        SwithunLog.d("findUsbFile return null")
        return null
    }

    override fun reduce(action: Action) {
        when (action) {
            is Action.ClickFile -> clickFile(action.file)
            is Action.ClickFolder -> clickFolder(action.folder)
            Action.RefreshBasePathListFromRemote -> refreshBasePathListFromRemote()
            is Action.FindUsbFile -> findUsbFileForName(action)
            is Action.GetUsbFileByPiece -> getUsbFileByPiece(action)
            Action.GetStorageList -> getStorageList()
            is Action.GetFileListOfStorage -> getBaseFileListOfStorage(action)
            is Action.ClickFileV2 -> clickFileV2(action)
            is Action.ClickFolderV2 -> clickFolderV2(action)
        }
    }

    private fun clickFolderV2(action: Action.ClickFolderV2) {
        viewModelScope.launch(Dispatchers.IO) {
            val folder = action.folder
            if (folder.isOpening) {
                action.folder.isOpening = false
            } else {
                val fileList = repository.getBaseFileListOfStorage(
                    Action.GetFileListOfStorage(
                        action.folder.storage,
                        action.folder.path
                    )
                )
                action.folder.isOpening = true
                action.folder.children = fileList.toList().toPathItem(action.folder.storage)
            }

            val rememberPathList = innerUISate.pathList
            innerUISate.pathList = mutableListOf()
            innerUISate.pathList = rememberPathList

        }
    }

    private fun clickFileV2(action: Action.ClickFileV2) {
    }

    private fun getBaseFileListOfStorage(action: Action.GetFileListOfStorage) {
        viewModelScope.launch(Dispatchers.IO) {
            val fileList = repository.getBaseFileListOfStorage(action)
            val uiFileList = fileList.toList().toPathItem(action.s)
            innerUISate.pathList = uiFileList
        }
    }

    private fun getStorageList() {
        viewModelScope.launch(Dispatchers.IO) {
            val storages = repository.getServerStorage()
            innerUISate.storageList = storages
        }
    }

    private fun getUsbFileByPiece(action: Action.GetUsbFileByPiece) {
        val datas = action.data.content.split(";");
        val pos = datas[0]
        val path = datas[1]


        val file = findUsbFile(path) ?: run {
            vmCollection?.connectKernelVM?.reduce(
                ConnectKernelViewModel.Action.ServerGetAndroidUsbFileByPieceFileManagerResponse(
                    ByteBuffer.allocate(0),
                    action.data.uuid,
                    pos,
                )
            )
            return
        }


        // 从pos开始读取 1024 * 1024 字节
        val frameSize = ServerConfig.fileFrameSize.toLong() * 1024L
        val leftFileSize = file.length - pos.toLong()
        val bufferSize = Math.min(frameSize, leftFileSize)
        SwithunLog.d("getUsbFileByPiece pos: ${pos} path ${path} fileLength : ${file.length} bfferize ${bufferSize}")
        val buffer = ByteBuffer.allocate(bufferSize.toInt())
        file.read(pos.toLong(), buffer)


        vmCollection?.connectKernelVM?.reduce(
            ConnectKernelViewModel.Action.ServerGetAndroidUsbFileByPieceFileManagerResponse(
                buffer,
                action.data.uuid,
                pos,
            )
        )

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

            val oldUploadList = innerUISate.uploadRootPathList
            innerUISate.uploadRootPathList = mutableListOf()
            innerUISate.uploadRootPathList = oldUploadList.toMutableList()
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
            innerUISate.pathList = addMainToFirst(remoteRepository.getBasePathList().toMutableList())
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

fun List<FileItem>.toPathItem(s: Storage): List<PathItem> {
    return this.map { item ->
        item.toPathItem(s)
    }
}

fun FileItem.toPathItem(s: Storage): PathItem {
    return if (this.isDir) {
        PathItem.FolderItem(
            this.name,
            this.path,
            s,
            emptyList()
        )
    } else {
        PathItem.FileItem(
            this.name,
            this.path,
            s,
        )
    }
}