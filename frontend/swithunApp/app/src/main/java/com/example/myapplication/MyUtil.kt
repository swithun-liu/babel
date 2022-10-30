package com.example.myapplication

import com.google.gson.Gson
import com.google.gson.GsonBuilder

fun Any.convert2Json(): String? {
    val gson: Gson = Gson()
    return gson.toJson(this)
}