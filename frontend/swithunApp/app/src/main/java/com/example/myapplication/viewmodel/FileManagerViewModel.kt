package com.example.myapplication.viewmodel

import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.myapplication.ActivityVar
import com.example.myapplication.SwithunLog
import com.example.myapplication.model.KernelConnectJson
import com.example.myapplication.nullCheck
import com.example.myapplication.websocket.RawData
import com.google.gson.Gson
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicLong

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

    fun getBasePathList(){
        activityVar?.wordsVM?.let {
            val uuid = UUID.randomUUID().toString()
            val j = KernelConnectJson(
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
}

sealed class PathItem(val path: String) {
    val name = path
    class FolderItem(path: String, var children: List<PathItem>) : PathItem(path) {
        var isOpening = false
    }
    class FileItem(path: String) : PathItem(path)
}