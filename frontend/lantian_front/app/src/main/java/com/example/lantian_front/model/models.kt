package com.example.lantian_front.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.lang.Exception

class Storage(
    @SerializedName("name")
    val name: String,
    @SerializedName("id")
    val id: String,
    @SerializedName("type")
    val type: Int,
    @SerializedName("base_path")
    val basePath: String
)

enum class StorageType(val value: Int) {
    LOCAL_INNER(0),
    X_EXPLORE(1)
}

inline fun <reified T> String.toObject() : T? {
    return try {
        Gson().fromJson(this, T::class.java)
    } catch (e: Exception) {
        null
    }
}

inline fun <reified T> T.toJson(): String {
    return Gson().toJson(this)
}