package com.example.lantian_front.model

import android.os.Environment
import com.example.lantian_front.MainActivity
import com.example.lantian_front.SwithunLog
import com.example.lantian_front.nullCheck

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