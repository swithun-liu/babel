package com.example.myapplication.model

import android.app.Activity
import com.example.myapplication.util.WebUtil

class KernelConfig(activity: () -> Activity) {
    val kernelIP = WebUtil.getLocalIPAddress(activity())
    val kernelPort = 8088
    val kernelHost = "$kernelIP:$kernelPort"
}