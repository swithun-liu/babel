package com.swithun.lantian

import com.example.lantian_front.SwithunLog

object FrontEndSDK {

    @JvmStatic
    val callback = object : Callback {
    }

    init {
        // 因为等会儿编译的so产物为librust_android_libs.so，所以此处加载命名如下
        SwithunLog.d("FrontEndSDK init")
        try {
            System.loadLibrary("lantian_frontend_lib")
        } catch (e: Exception) {
            SwithunLog.d("FrontEndSDK init err $e")
        }
    }

    fun <R: Response> request(request: Request<R>): R {
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
        }
    }

    private external fun connectServer(serverIp: String, callback: Callback): Boolean
    private external fun searchServer(lanIp: String): Array<String>

}

interface Callback: BaseCallBack {
    fun result() {
        SwithunLog.d("Callback result")
    }
}

interface BaseCallBack
interface JsonCallback: BaseCallBack {
    fun result(op: Int, json: String)
}
