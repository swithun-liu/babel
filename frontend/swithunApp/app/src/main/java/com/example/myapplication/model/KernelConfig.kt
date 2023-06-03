package com.example.myapplication.model

import android.app.Activity
import com.example.myapplication.util.WebUtil

object KernelConfig {
    var kernelIP: String = ""
        private set
    private const val kernelPort: Int = 8088
    val kernelHost: String = "$kernelIP:$kernelPort"


    fun init(activity: Activity) {
        kernelIP =  WebUtil.getLocalIPAddress(activity)
    }
    object KernelPath {
        object ConnectPath {
            const val connect = "connect"
        }
    }
}