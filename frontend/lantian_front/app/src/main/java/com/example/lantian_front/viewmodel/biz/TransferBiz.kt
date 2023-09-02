package com.example.lantian_front.viewmodel.biz

import com.example.lantian_front.model.MessageTextDTO
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