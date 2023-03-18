package com.example.myapplication.viewmodel

import com.example.myapplication.model.MessageDTO
import java.util.*

object TransferBiz {
    fun buildTransferData(text: String): MessageDTO {
        val uuid = UUID.randomUUID().toString()
        return MessageDTO(
            uuid,
            MessageDTO.OptionCode.TRANSFER_DATA.code,
            text,
            MessageDTO.ContentType.TEXT.type
        )
    }
}