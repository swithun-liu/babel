package com.example.myapplication.viewmodel

import com.example.myapplication.model.TransferData
import java.util.*

object TransferBiz {
    fun buildTransferData(text: String): TransferData {
        val uuid = UUID.randomUUID().toString()
        return TransferData(
            uuid,
            TransferData.OptionCode.TRANSFER_DATA.code,
            text,
            TransferData.ContentType.TEXT.type
        )
    }
}