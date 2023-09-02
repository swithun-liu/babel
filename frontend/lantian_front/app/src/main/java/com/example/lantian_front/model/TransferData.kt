package com.example.lantian_front.model

sealed class TransferData {
    data class TextData(val text: String): TransferData()

    data class ImageData(val imageName: String): TransferData()
}