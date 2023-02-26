package com.example.myapplication.viewmodel

import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.myapplication.ActivityVar
import com.example.myapplication.model.KernelAndFrontEndJson
import com.example.myapplication.nullCheck
import com.example.myapplication.util.UrlEncodeParams
import com.example.myapplication.util.getRequest
import com.example.myapplication.util.getRequestWithOriginalResponse
import com.example.myapplication.websocket.RawData
import com.google.gson.Gson
import com.koushikdutta.async.http.HttpUtil
import java.io.File
import java.util.*

class FileManagerViewModel: ViewModel() {

    val fileBasePath: String = Environment.getExternalStorageDirectory().absolutePath
    var pathList: List<PathItem> by mutableStateOf(listOf())
    var activityVar: ActivityVar? = null

    init {
        val basePath = File(fileBasePath)
        pathList = basePath.listFiles()?.map2Items() ?: emptyList()
    }

    fun init(activityVar: ActivityVar) {
        this.activityVar = activityVar
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
        // todo 看看怎么做不用这样赋值
        val oldList = pathList
        pathList = mutableListOf()
        pathList = oldList
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

    private fun Array<File>.map2LocalItems(): List<LocalPathItem> {
        return this.map {
            LocalPathItem(
                it.path,
                if (it.isFile) LocalPathItem.PathType.FILE.type else LocalPathItem.PathType.Folder.type
            )
        }
    }

    fun getBasePathListFromLocal(): List<LocalPathItem> {
        val basePath = File(fileBasePath)
        return basePath.listFiles()?.map2LocalItems() ?: emptyList()
    }

    fun getBasePathList(){
        activityVar?.wordsVM?.let {
            val uuid = UUID.randomUUID().toString()
            val j = KernelAndFrontEndJson(
                uuid,
                1,
                "base path list"
            )
            val gson = Gson()
            val json = gson.toJson(j)
            val data = RawData(json)
            it.sendCommand(data)
        }
    }

    suspend fun getBasePathListByHttp() {
        val urlEncodeParams = UrlEncodeParams().apply {
            put("path", "base")
        }
        getRequestWithOriginalResponse("http://192.168.0.109:8088/get_path_list", urlEncodeParams)?.body?.string().nullCheck("response: ", true)
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
        Folder(1)
    }
}