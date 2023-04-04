package com.example.myapplication.model

import com.example.myapplication.SwithunLog
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okio.ByteString
import java.nio.ByteBuffer

data class MessageDTO(
    @SerializedName("uuid") val uuid: String,
    /** [OptionCode] */
    @SerializedName("code") val code: Int,
    @SerializedName("content") val content: String,
    /** [ContentType] */
    @SerializedName("content_type") val content_type: Int
) {

    fun toJsonStr(): String {
        val gson = Gson();
        return gson.toJson(this)
    }

    enum class ContentType(val type: Int) {
        TEXT(0),
        IMAGE(1),
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

data class MessageBinaryDTO(
    val contentId: String,
    val seq: Int,
    val payload: ByteString
) {
    fun toByteArray(): ByteArray {
        SwithunLog.d("contentId: $contentId")
        val contentIdBytes: ByteArray = contentId.toByteArray(Charsets.UTF_8).copyOf(36)
        SwithunLog.d("contentIdBytes $contentIdBytes")
        val seqBytes: ByteArray = ByteBuffer.allocate(4).putInt(seq).array()
        val payloadBytes = payload.toByteArray()
        val totalLength = contentIdBytes.size + seqBytes.size + payloadBytes.size
        val result = ByteArray(totalLength)
        System.arraycopy(contentIdBytes, 0, result, 0, contentIdBytes.size)
        System.arraycopy(seqBytes, 0, result, contentIdBytes.size, seqBytes.size)
        System.arraycopy(payloadBytes, 0, result, contentIdBytes.size + seqBytes.size, payloadBytes.size)
        return result
    }
}