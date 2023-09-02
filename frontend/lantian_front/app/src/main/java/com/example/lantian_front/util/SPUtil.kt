package com.example.lantian_front.util

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.example.lantian_front.nullCheck

object SPUtil {

    fun getSP(activity: Activity?): SharedPreferences? {
        return activity?.getPreferences(Context.MODE_PRIVATE).nullCheck("get SP")
    }

    fun putString(activity: Activity?, key: String, value: String): Boolean {
        return commonPut(activity, key, value)
    }

    fun getString(activity: Activity?, key: String): String? {
        return getSP(activity)?.getString(key, "")
    }

    fun putInt(activity: Activity?, key: String, value: Int): Boolean {
        return commonPut(activity, key, value)
    }

    fun getInt(activity: Activity?, key: String): Int? {
        return getSP(activity)?.getInt(key, -1)
    }

    private fun commonPut(activity: Activity?, key: String, value: Any): Boolean {
        return getSP(activity)?.edit()?.let { editor ->
            when (value) {
                is String -> {
                    editor.putString(key, value)
                }
                is Int -> {
                    editor.putInt(key, value)
                }
                else -> return false
            }
            editor.apply()
            true
        } ?: false
    }

    object Conan {

        private const val CurrentConan = "CurrentConan"

        fun putCurrentConan(activity: Activity?, pos: Int) {
            putInt(activity, CurrentConan, pos)
        }

        fun getCurrentConan(activity: Activity?): Int? {
            return getInt(activity, CurrentConan)
        }

    }

    object ServerSetting {
        private const val LastTimeConnectServer = "LastTimeConnectServer"

        fun getLastTimeConnectServer(activity: Activity): String? {
            return getString(activity, LastTimeConnectServer)
        }

        fun putLastTimeConnectServer(activity: Activity, lastTimeConnectServer: String) {
            putString(activity, LastTimeConnectServer, lastTimeConnectServer)
        }
    }

    object PathSetting {
        private const val uploadFileRootDir = "uploadFileRootDir"

        fun getUploadFileRootDir(activity: Activity): String? {
            return getString(activity, uploadFileRootDir)
        }

        fun putUploadFileRootDir(activity: Activity, uploadFileRootDir: String) {
            putString(activity, uploadFileRootDir, uploadFileRootDir)
        }
    }

}