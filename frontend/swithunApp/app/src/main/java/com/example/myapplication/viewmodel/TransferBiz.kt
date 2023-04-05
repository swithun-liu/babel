package com.example.myapplication.viewmodel

import com.example.myapplication.model.MessageTextDTO
import java.util.*

object TransferBiz {
    fun buildTransferData(text: String): MessageTextDTO {
        return MessageTextDTO(
            getUUID(),
            MessageTextDTO.OptionCode.TRANSFER_DATA.code,
            text,
            MessageTextDTO.ContentType.TEXT.type
        )
    }

    fun buildRequestTransferData(fileName: String): MessageTextDTO {
        return MessageTextDTO(
            getUUID(),
            MessageTextDTO.OptionCode.REQUEST_TRANSFER_DATA.code,
            fileName,
            MessageTextDTO.ContentType.TEXT.type
        )
    }

    private fun getUUID(): String {
       return UUID.randomUUID().toString()
    }
}