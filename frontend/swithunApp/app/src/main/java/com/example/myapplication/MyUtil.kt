package com.example.myapplication

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder

fun Any?.convert2Json(): String? {
    if (this == null) return null

    val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    return gson.toJson(this)
}

object SwithunLog {

    private fun generateMsg(msg: String?): String {

        val elements = Thread.currentThread().stackTrace
        var fullMessage = msg ?: return "null"

        elements.getOrNull(4)?.let { element ->
            val className = element.className ?: "err class name"
            val functionName = element.methodName ?: "err method name"
            val lineNumber = element.lineNumber
            val fullMsg = "{$className}#[$functionName]#($lineNumber): $msg"
            fullMessage = fullMsg
        }
        return fullMessage
    }

    fun d(msg: Any?) {
        Log.d("swithun-xxxx", generateMsg(msg.convert2Json()))
    }

    fun e(msg: Any?) {
        Log.e("swithun-xxxx", generateMsg(msg.convert2Json()))
    }
}


fun<T> T.nullCheck(msg: String, needLogOriginalResult: Boolean = false): T {
    if (this == null) {
        SwithunLog.e("#(null check) $msg: err !!")
    }
    if (needLogOriginalResult) {
        SwithunLog.d("#(null check) $msg: ${this.convert2Json()}")
    }
    return this
}
