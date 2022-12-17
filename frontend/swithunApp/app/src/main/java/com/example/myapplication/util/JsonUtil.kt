package com.example.myapplication.util

import org.json.JSONArray
import org.json.JSONObject

fun JSONObject.safeGetJSONObject(name: String): JSONObject? {
    return if (this.has(name)) {
        this.getJSONObject(name)
    } else {
        null
    }
}

fun JSONObject.safeGetString(name: String): String? {
    return if (this.has(name)) {
        this.getString(name)
    } else {
        null
    }
}

fun JSONObject.safeGetJsonArray(name: String): JSONArray? {
    return if (this.has(name)) {
        this.getJSONArray(name)
    } else {
        null
    }
}