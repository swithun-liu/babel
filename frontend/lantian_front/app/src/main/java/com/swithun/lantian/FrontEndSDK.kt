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

    external fun connectServer(callback: Callback)
}

interface Callback {
    fun result() {
        SwithunLog.d("Callback result")
    }
}