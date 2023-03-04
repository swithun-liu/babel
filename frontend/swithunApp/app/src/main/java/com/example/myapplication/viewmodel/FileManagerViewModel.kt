package com.example.myapplication.viewmodel

import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.myapplication.ActivityVar
import com.example.myapplication.SwithunLog
import com.example.myapplication.model.KernelAndFrontEndJson
import com.example.myapplication.nullCheck
import com.example.myapplication.util.UrlEncodeParams
import com.example.myapplication.util.getRequest
import com.example.myapplication.util.getRequestWithOriginalResponse
import com.example.myapplication.websocket.RawData
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.reflect.TypeToken
import com.koushikdutta.async.http.HttpUtil
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.lang.Exception
import java.util.*

class FileManagerViewModel : ViewModel() {

    val fileBasePath: String = Environment.getExternalStorageDirectory().absolutePath
    var pathList: List<PathItem> by mutableStateOf(listOf())
    var activityVar: ActivityVar? = null

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
                folder.children =
                    parent.listFiles()?.map2Items().nullCheck("children: ") ?: emptyList()
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

    fun getBasePathList() {
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
        val pathListJsonStr: String = getRequestWithOriginalResponse(
            "http://192.168.0.109:8088/get_path_list",
            urlEncodeParams
        )?.body?.string().nullCheck("response: ", true)
            ?: return
        try {
            val pathItemArrayList = JSONArray(pathListJsonStr)
            val pathItemList = mutableListOf<PathItem>()
            for (i in 0 until pathItemArrayList.length()) {
                val it = pathItemArrayList.get(i) as? JSONObject ?: continue
                val fileTypeInt = it.getInt("fileType")
                val fileType =
                    LocalPathItem.PathType.fromValue(fileTypeInt).nullCheck("解析的fileType") ?: return
                val path = it.getString("path")
                val item = when (fileType) {
                    LocalPathItem.PathType.FILE -> {
                        PathItem.FileItem(path)
                    }
                    LocalPathItem.PathType.Folder -> {
                        PathItem.FolderItem(path, emptyList())
                    }
                }
                pathItemList.add(item)
            }
            pathList = pathItemList

        } catch (e: Exception) {
            SwithunLog.e("http path 解析失败")
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

data class LocalPathItem(val path: String, val fileType: Int) {

    enum class PathType(val type: Int) {
        FILE(0),
        Folder(1);

        companion object {
            fun fromValue(value: Int): PathType? = values().find { it.type == value }
        }

    }

}