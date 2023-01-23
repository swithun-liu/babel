package com.example.myapplication.util

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*

/**
 * 动态获取权限，Android 6.0 新特性，一些保护权限，除了要在AndroidManifest中声明权限，还要使用如下代码动态获取
 */
object AuthChecker {

    fun checkWriteExternalStorage(activity: Activity) {
        if (Build.VERSION.SDK_INT >= 23) {
            val REQUEST_CODE_CONTACT = 101
            val permissions = arrayOf<String>(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            //验证是否许可权限
            for (str in permissions) {
                if (activity.checkSelfPermission(str!!) != PackageManager.PERMISSION_GRANTED) {
                    //申请权限
                    activity.requestPermissions(permissions, REQUEST_CODE_CONTACT)
                    return
                }
            }
        }
    }

}