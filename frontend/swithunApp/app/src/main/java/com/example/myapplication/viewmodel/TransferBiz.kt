package com.example.myapplication.viewmodel

import com.example.myapplication.model.MessageTextDTO
import java.util.*

object TransferBiz {
    fun buildPostDTO(text: String): MessageTextDTO {
        return MessageTextDTO(
            getUUID(),
            MessageTextDTO.OptionCode.POST_SESSION_TEXT.code,
            text,
            MessageTextDTO.ContentType.TEXT.type
        )
    }

    fun buildGetDTO(fileName: String): MessageTextDTO {
        return MessageTextDTO(
            getUUID(),
            MessageTextDTO.OptionCode.POST_SESSOIN_FILE.code,
            fileName,
            MessageTextDTO.ContentType.TEXT.type
        )
    }

    private fun getUUID(): String {
       return UUID.randomUUID().toString()
    }
}