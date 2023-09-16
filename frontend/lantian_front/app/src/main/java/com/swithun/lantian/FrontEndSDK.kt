package com.swithun.lantian

import com.example.lantian_front.SwithunLog

object FrontEndSDK {

    @JvmStatic
    val callback = object : Callback {}

    init {
        // 因为等会儿编译的so产物为librust_android_libs.so，所以此处加载命名如下
        SwithunLog.d("FrontEndSDK init")
        try {
            System.loadLibrary("lantian_frontend_lib")
        } catch (e: Exception) {
            SwithunLog.d("FrontEndSDK init err $e")
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <R : Response> request(request: Request<R>): R {
        return when (request) {
            is Request.ConnectServer -> request.createResponse(
                Response.ConnectServerRsp(
                    connectServer(request.serverIp, request.callback)
                )
            ) as R

            is Request.SearchServer -> request.createResponse(
                Response.SearchServerRsp(
                    searchServer(
                        request.subNet
                    )
                )
            ) as R

            is Request.GetStorageList -> request.createResponse(
                Response.GetStorageRsp(getStorageList())
            ) as R

            is Request.GetBaseFileOfStorage -> request.createResponse(
                Response.GetBaseFileOfStorageRsp(
                    getBaseFileListOfStorage(request.storage.type, request.storage.basePath)
                )
            ) as R
        }
    }

    private external fun connectServer(serverIp: String, callback: JsonCallback): Boolean
    private external fun searchServer(lanIp: String): Array<String>
    private external fun getStorageList(): Array<String>
    private external fun getBaseFileListOfStorage(type: Int, basePath: String): Array<String>

}

interface Callback : BaseCallBack {
    fun result() {
        SwithunLog.d("Callback result")
    }
}

interface BaseCallBack
interface JsonCallback : BaseCallBack {
    fun result(op: String, json: String)
}
