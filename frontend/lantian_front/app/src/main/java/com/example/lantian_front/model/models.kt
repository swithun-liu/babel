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
) {
    companion object {
        fun getPreview(): Storage = Storage(
            "name",
            "id",
            0,
            "path"
        )
    }
}

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


/**
 * #[derive(Serialize, Deserialize, Debug)]
    pub struct FileItem {
    pub name: String,
    pub path: String,
    pub is_dir: bool,
}

 */

class FileItem(
    @SerializedName("name")
    val name: String,
    @SerializedName("path")
    val path: String,
    @SerializedName("is_dir")
    val isDir: Boolean
) {
    override fun toString(): String {
        return this.toJson()
    }
}