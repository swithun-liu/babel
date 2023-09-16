package com.example.lantian_front.viewmodel.serversetting

import android.os.Environment
import com.swithun.lantian.FrontEndSDK
import com.swithun.lantian.Request
import kotlinx.coroutines.CoroutineScope
import com.example.lantian_front.framework.Result
import com.example.lantian_front.framework.toOkResult
import com.example.lantian_front.framework.toResult
import com.example.lantian_front.model.Storage
import com.example.lantian_front.model.StorageType
import com.example.lantian_front.model.toObject
import com.swithun.lantian.JsonCallback

class Repository() {

    fun searchServer(action: Action.SearchServer): Result<Array<String>, Unit> {
        val result = FrontEndSDK.request(Request.SearchServer(action.lanIp))

        val ips = result.ips
        return ips.toOkResult()
    }

    fun connectServer(action: Action.ConnectServer, jsonCallback: JsonCallback): Result<Unit, Unit> {
        val result = FrontEndSDK.request(Request.ConnectServer(action.ip,  jsonCallback))
        return result.result.toResult()
    }

    fun getServerStorage(): List<Storage> {

        val innerLocalBasePath = Environment.getExternalStorageDirectory().absolutePath
        val storage = mutableListOf(
            Storage(
                "内部存储",
                "1",
                StorageType.LOCAL_INNER.value,
                innerLocalBasePath
            )
        )
        val result = FrontEndSDK.request(Request.GetStorageList())
        val sdkStorage = result.storages.mapNotNull {  json ->
            json.toObject<Storage>()
        }

        return storage + sdkStorage
    }

}