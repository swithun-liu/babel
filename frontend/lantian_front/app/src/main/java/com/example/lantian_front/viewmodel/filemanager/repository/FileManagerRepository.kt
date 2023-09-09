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

        val innerLocalBasePath = fileBasePath
        val storage = mutableListOf(
            Storage(
                "内部存储",
                "1",
                StorageType.LOCAL_INNER.value,
                innerLocalBasePath
            )
        )
        val result = FrontEndSDK.request(Request.GetStorage())
        val sdkStorage = result.storages.mapNotNull { json ->
            json.toObject<Storage>()
        }

        return (storage + sdkStorage).also {
            SwithunLog.d("$it", TAG, "getServerStorage")
        }
    }


    private fun getBaseFileListOfStorage(action: Action.GetBaseFileListOfStorage) {

    }


    companion object {
        private const val TAG = "FileManagerRepository"
    }

}

