package com.example.myapplication.model

import com.google.gson.annotations.SerializedName

data class KernelConnectJson(
    @SerializedName("uuid") val uuid: String,
    @SerializedName("code") val code: Int,
    @SerializedName("content") val content: String,
)