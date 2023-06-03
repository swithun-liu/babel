package com.example.myapplication.model

import android.os.Environment
import com.example.myapplication.MainActivity
import com.example.myapplication.SwithunLog
import com.example.myapplication.nullCheck

object PathConfig {

    var appExternalPath: String = ""

    const val postFileClientDownloadPath = "/babel/transfer/download"
    const val postFileServerCachePath = "/babel/transfer/cache"
    const val postUploadFileCachePath = "/babel/upload"


    fun init(activity: MainActivity) {
        appExternalPath =
            try {
                activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.path.nullCheck(
                    "appExternalPath",
                    true
                ) ?: ""
            } catch (e: java.lang.Exception) {
                SwithunLog.d("get appExternalPath failed : $e")
                ""
            }

    }

}