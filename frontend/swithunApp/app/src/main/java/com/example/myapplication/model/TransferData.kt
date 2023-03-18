package com.example.myapplication.model

sealed class TransferData {
    data class TextData(val text: String): TransferData()
}