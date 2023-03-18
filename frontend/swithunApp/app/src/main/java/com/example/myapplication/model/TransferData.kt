package com.example.myapplication.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class TransferData(
    @SerializedName("uuid") val uuid: String,
    @SerializedName("code") val code: Int,
    @SerializedName("content") val content: String,
    @SerializedName("content_type") val content_type: Int
) {

    fun toJsonStr(): String {
        val gson = Gson();
        return gson.toJson(this)
    }

    enum class ContentType(val type: Int) {
        TEXT(0),
        PNG(1),
    }

    enum class OptionCode(val code: Int) {
        GET_BASE_PATH_LIST_REQUEST(1),
        GET_BASE_PATH_LIST_RESPONSE(2),
        GET_CHILDREN_PATH_LIST_REQUEST(3),
        GET_CHILDREN_PATH_LIST_RESPONSE(4),
        TRANSFER_DATA(5)
        ;

        companion object {
            fun fromValue(code: Int): OptionCode? = values().find { it.code == code }
        }
    }

}