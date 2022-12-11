package com.example.myapplication

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder

fun Any.convert2Json(): String? {
    val gson: Gson = Gson()
    return gson.toJson(this)
}

object SwithunLog {

    fun d(msg: String) {
        val elements = Thread.currentThread().stackTrace
        var fullMessage = msg
        elements.getOrNull(3)?.let { element ->
            val className = element.className ?: "err class name"
            val functionName = element.methodName ?: "err method name"
            val lineNumber = element.lineNumber ?: "err line number"
            val fullMsg = "{$className}#[$functionName]#($lineNumber): $msg"
            fullMessage = fullMsg
        }
        Log.d("swithun-xxxx", fullMessage)
    }
}
