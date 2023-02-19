package com.swithun.liu

object ServerSDK {
    init {
        // 因为等会儿编译的so产物为librust_android_libs.so，所以此处加载命名如下
         System.loadLibrary("rust_server_android_libs")
    }

    external fun getTestStr(): String
    external fun getTestStrWithInput(input: String): String
    external suspend fun startSever()
}