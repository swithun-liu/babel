package com.example.lantian_front.viewmodel.filemanager.repository

import com.example.lantian_front.SwithunLog
import com.example.lantian_front.model.Storage
import com.example.lantian_front.model.StorageType
import com.example.lantian_front.model.toObject
import com.example.lantian_front.viewmodel.filemanager.Action
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


    fun getBaseFileListOfStorage(action: Action.GetBaseFileListOfStorage): Array<String> {
        val result = FrontEndSDK.request(Request.GetBaseFileOfStorage(action.s))
        return result.fileList
    }


    companion object {
        private const val TAG = "FileManagerRepository"
    }

}

