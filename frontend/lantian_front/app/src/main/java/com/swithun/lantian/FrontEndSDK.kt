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

    fun request(request: Request): Any {
        return when (request) {
            is Request.ConnectServer -> connectServer(request.callback)
            is Request.SearchServer -> Request.SearchServer.Response(searchServer(request.subNet))
        }
    }

    private external fun connectServer(callback: Callback)
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
