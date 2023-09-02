package com.example.myapplication.util

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import com.example.myapplication.SwithunLog

/**
 * 动态获取权限，Android 6.0 新特性，一些保护权限，除了要在AndroidManifest中声明权限，还要使用如下代码动态获取
 */
object AuthChecker {

    fun checkWriteExternalStorage(activity: Activity) {
        SwithunLog.d("权限检查")
        if (Build.VERSION.SDK_INT >= 23) {
            val REQUEST_CODE_CONTACT = 101
            val permissions = arrayOf<String>(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                Manifest.permission.MANAGE_EXTERNAL_STORAGE
            )


            activity.requestPermissions(
                arrayOf<String>(Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS),
                REQUEST_CODE_CONTACT
            )
            //验证是否许可权限
            for (str in permissions) {
                if (activity.checkSelfPermission(str!!) != PackageManager.PERMISSION_GRANTED) {
                    //申请权限
                    SwithunLog.d("没有权限")
                    activity.requestPermissions(permissions, REQUEST_CODE_CONTACT)
                } else {
                    SwithunLog.d("有权限了")
                }
            }
        }
    }
}