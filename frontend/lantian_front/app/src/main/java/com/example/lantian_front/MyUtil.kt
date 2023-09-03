package com.example.lantian_front

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder

fun Any?.convert2Json(): String? {
    if (this == null) return null
    if (this is String) return this

    val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    return null
//    return try {
//        StringEscapeUtils.unescapeJson(gson.toJson(this))
//    } catch (e: Exception) {
//        Log.e("swithun-xxxx", "convert2Json err: ${e.message}")
//        null
//    }
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

    fun d(msg: Any?, description: String) {
        Log.d("swithun-xxxx", generateMsg(description + " : " + msg.convert2Json()))
    }

    fun d(msg: Any?, vararg description: String) {
        val description = description.joinToString(separator = " # ")
        Log.d("swithun-xxxx", generateMsg(description + " : " + msg.convert2Json()))
    }

    fun e(msg: Any?) {
        Log.e("swithun-xxxx", generateMsg(msg.convert2Json()))
    }
}


fun<T> T?.nullCheck(msg: String, needLogOriginalResult: Boolean = false): T? {
    if (this == null) {
        SwithunLog.e("#(null check) $msg: err !!")
    }
    if (needLogOriginalResult) {
        SwithunLog.d("#(null check) $msg: ${this.convert2Json()}")
    }
    return this
}
