package com.example.lantian_front.util

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

fun JSONObject.safeGetLong(name: String): Long? {
    return if (this.has(name)) {
        this.getLong(name)
    } else {
        null
    }
}
