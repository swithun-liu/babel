package com.example.myapplication.viewmodel.biz

import com.example.myapplication.model.MessageTextDTO
import java.util.*

object TransferBiz {
    fun buildPostDTO(text: String): MessageTextDTO {
        return MessageTextDTO(
            getUUID(),
            MessageTextDTO.OptionCode.MESSAGE_TO_SESSION.code,
            text,
            MessageTextDTO.ContentType.TEXT.type
        )
    }

    fun buildGetDTO(filePath: String): MessageTextDTO {
        return MessageTextDTO(
            getUUID(),
            MessageTextDTO.OptionCode.CLIENT_REQUEST_SESSION_FILE.code,
            filePath,
            MessageTextDTO.ContentType.TEXT.type
        )
    }

    private fun getUUID(): String {
       return UUID.randomUUID().toString()
    }
}