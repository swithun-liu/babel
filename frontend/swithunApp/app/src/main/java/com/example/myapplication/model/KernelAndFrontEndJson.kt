package com.example.myapplication.model

import com.google.gson.annotations.SerializedName

data class KernelAndFrontEndJson(
    @SerializedName("uuid") val uuid: String,
    @SerializedName("code") val code: Int,
    @SerializedName("content") val content: String,
    @SerializedName("content_type") val content_type: Int
) {
    enum class ContentType(val v: Int) {
        TEXT(0),
        PNG(1),
    }
}