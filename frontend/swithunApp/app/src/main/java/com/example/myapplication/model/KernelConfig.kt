package com.example.myapplication.model

import android.app.Activity
import com.example.myapplication.util.WebUtil

class KernelConfig(
    activity: () -> Activity,
    val kernelIP: String = WebUtil.getLocalIPAddress(activity()),
    val kernelPort: Int = 8088,
    val kernelHost: String = "$kernelIP:$kernelPort",
) {

    object KernelPath {
        object ConnectPath {
            const val connect = "connect"
        }
    }
}