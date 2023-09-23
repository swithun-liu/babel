package com.example.myapplication.util

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.*
import com.example.myapplication.SwithunLog
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

/**
 * 动态获取权限，Android 6.0 新特性，一些保护权限，除了要在AndroidManifest中声明权限，还要使用如下代码动态获取
 */
object AuthChecker {

    fun checkWriteExternalStorage(activity: Activity, code: Int): Boolean {
        val REQUEST_CODE_CONTACT = code

        SwithunLog.d("权限检查")

        var needRequst = false

        return if (Build.VERSION.SDK_INT >= 23) {
            val permissions = arrayOf<String>(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            )

            //验证是否许可权限

            for (str in permissions) {
                if (activity.checkSelfPermission(str!!) != PackageManager.PERMISSION_GRANTED) {
                    //申请权限
                    SwithunLog.d("没有权限")
                    needRequst = true
                } else {
                    SwithunLog.d("有权限了")
                }
            }

            if (needRequst) {
                activity.requestPermissions(permissions, REQUEST_CODE_CONTACT)
                false
            } else {
                true
            }
        } else {
            true
        }
    }


    private fun requestmanageexternalstorage_Permission(activity: Activity, code: Int): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 先判断有没有权限
            if (Environment.isExternalStorageManager()) {
//                Toast.makeText(
//                    this,
//                    "Android VERSION  R OR ABOVE，HAVE MANAGE_EXTERNAL_STORAGE GRANTED!",
//                    Toast.LENGTH_LONG
//                ).show()
                SwithunLog.d("usb 有externaManger权限")
                true
            } else {
//                Toast.makeText(
//                    this,
//                    "Android VERSION  R OR ABOVE，NO MANAGE_EXTERNAL_STORAGE GRANTED!",
//                    Toast.LENGTH_LONG
//                ).show()
                SwithunLog.d("usb 无externaManger权限")
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:" + activity.packageName)
                activity.startActivityForResult(intent, code)
                false
            }
        } else {
            true
        }
    }

    private fun showOpenDocumentTree(activity: Activity, rootPath: String, code: Int) {
        var intent: Intent? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val sm = activity.getSystemService(StorageManager::class.java)
            val volume = sm.getStorageVolume(File(rootPath))
            if (volume != null) {
                intent = volume.createAccessIntent(null)
            }
        }
        if (intent == null) {
            intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        }
        SwithunLog.d("MainActivity", "startActivityForResult...")
        activity.startActivityForResult(intent, code)
    }

    suspend fun checkUsb(
        activity: Activity,
        rootPath: String,
        resumer: (code: Int, continuation: CancellableContinuation<Boolean>) -> Unit,
    ) = suspendCancellableCoroutine<Boolean> {
        showOpenDocumentTree(activity, rootPath, ExternalUsb)
        resumer(ExternalUsb, it)
    }

    suspend fun checkWriteExternalStorageV2(
        activity: Activity,
        resumer: (code: Int, continuation: CancellableContinuation<Boolean>) -> Unit,
    ) = suspendCancellableCoroutine<Boolean> { continuaton: CancellableContinuation<Boolean> ->
        if (checkWriteExternalStorage(activity, REQUEST_CODE_CONTACT )) {
            continuaton.resume(true)
        } else {
            resumer(REQUEST_CODE_CONTACT, continuaton)
        }
    }

    suspend fun checkManagerExternalStorage(
        activity: Activity,
        resumer: (code: Int, continuation: CancellableContinuation<Boolean>) -> Unit,
    )= suspendCancellableCoroutine<Boolean> {
        if (requestmanageexternalstorage_Permission(activity, ExternalManager)){
            it.resume(true)
        } else {
            resumer(ExternalManager, it)
        }
    }

    const val REQUEST_CODE_CONTACT = 101
    const val ExternalManager = 102
    const val ExternalUsb = 103
}