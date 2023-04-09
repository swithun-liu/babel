package com.example.myapplication.model

import android.os.Environment
import com.example.myapplication.MainActivity
import com.example.myapplication.SwithunLog
import com.example.myapplication.nullCheck

class PathConfig(val activity: MainActivity) {
    val appExternalPath: String?
        get() =
            try {
                activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.path.nullCheck("appExternalPath", true)
            } catch (e: java.lang.Exception) {
                SwithunLog.d("get appExternalPath failed : $e")
                null
            }

    val postFileClientDownloadPath = "/babel/transfer/download"
    val postFileServerCachePath = "/babel/transfer/cache"
}