package com.example.lantian_front.viewmodel

import com.example.lantian_front.Config
import com.example.lantian_front.SwithunLog
import com.example.lantian_front.model.ServerConfig
import com.example.lantian_front.nullCheck
import com.example.lantian_front.util.UrlEncodeParams
import com.example.lantian_front.util.getRequestWithOriginalResponse
import com.example.lantian_front.viewmodel.filemanager.LocalPathItem
import com.example.lantian_front.viewmodel.filemanager.PathItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.lang.Exception

class FileManagerHTTPRepository() {

    private val GET_PATH_LIST_URL get() = "http://${Config.serverConfig.serverHost}/${ServerConfig.ServerPath.GetPathList.path}"

    suspend fun getBasePathList(): List<PathItem> {
        return getChildrenPathList("base")
    }

    suspend fun getChildrenPathList(parentPath: String): List<PathItem> {
        val urlEncodeParams = UrlEncodeParams().apply {
            put(ServerConfig.ServerPath.GetPathList.paramPath, parentPath)
        }
        val pathListJsonStr = getRequestWithOriginalResponse(GET_PATH_LIST_URL, urlEncodeParams)?.body?.string()
            .nullCheck("response: ", true) ?: return emptyList()
        try {
            return parseJsonArray(pathListJsonStr)
        } catch (e: Exception) {
            SwithunLog.e("http path 解析失败")
        }
        return emptyList()
    }

    @Throws(Exception::class)
    private fun parseJsonArray(pathListJsonStr: String): List<PathItem> {
        val pathItemArrayList = JSONArray(pathListJsonStr)
        val pathItemList = mutableListOf<PathItem>()
        for (i in 0 until pathItemArrayList.length()) {
            val it = pathItemArrayList.get(i) as? JSONObject ?: continue
            val fileTypeInt = it.getInt("fileType")
            val fileType =
                LocalPathItem.PathType.fromValue(fileTypeInt).nullCheck("解析的fileType") ?: return emptyList()
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
        return pathItemList
    }

}

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