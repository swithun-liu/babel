package com.example.myapplication.model

enum class VideoExtension(val extensionName: String) {
    MP4("mp4"),
    MKV("mkv");

    companion object {
        fun fromValue(value: String): VideoExtension? = values().find { it.extensionName == value }

        fun isOneOf(value: String) = fromValue(value) != null
    }
}