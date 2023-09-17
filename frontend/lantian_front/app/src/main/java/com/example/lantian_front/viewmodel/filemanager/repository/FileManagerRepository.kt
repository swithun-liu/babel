package com.example.lantian_front.viewmodel.filemanager.repository

import com.example.lantian_front.SwithunLog
import com.example.lantian_front.model.FileItem
import com.example.lantian_front.model.Storage
import com.example.lantian_front.model.toObject
import com.example.lantian_front.viewmodel.filemanager.Action
import com.google.gson.Gson
import com.swithun.lantian.FrontEndSDK
import com.swithun.lantian.Request

class FileManagerRepository(private val fileBasePath: String) {

    fun getServerStorage(): List<Storage> {
        val result = FrontEndSDK.request(Request.GetStorageList())
        val sdkStorage = result.storages.mapNotNull { json ->
            json.toObject<Storage>()
        }

        return (sdkStorage).also {
            SwithunLog.d("$it", TAG, "getServerStorage")
        }
    }


    fun getBaseFileListOfStorage(action: Action.GetFileListOfStorage): Array<FileItem> {
        val result = FrontEndSDK.request(Request.GetFileOfStorage(action.s, action.filePath))
        SwithunLog.d(result.fileList, "FileManagerRepository", "getBaseFileListOfStorage")

        val gson = Gson()
        val fileList = gson.fromJson(result.fileList, Array<FileItem>::class.java)

        for (file in fileList) {
            SwithunLog.d("$file", "FileManagerRepository", "getBaseFileListOfStorage")
        }

        return fileList
    }


    companion object {
        private const val TAG = "FileManagerRepository"
    }

}

