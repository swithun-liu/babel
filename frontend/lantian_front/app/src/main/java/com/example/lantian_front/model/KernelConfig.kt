package com.example.lantian_front.model

import android.app.Activity
import com.example.lantian_front.util.WebUtil

object KernelConfig {
    var kernelIP: String = ""
        private set
    private const val kernelPort: Int = 8088
    val kernelHost: String
        get() ="$kernelIP:$kernelPort"


    fun init(activity: Activity) {
        kernelIP =  WebUtil.getLocalIPAddress(activity)
    }
    object KernelPath {
        object ConnectPath {
            const val connect = "connect"
        }
    }
}