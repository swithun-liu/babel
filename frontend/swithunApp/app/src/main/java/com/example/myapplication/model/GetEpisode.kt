package com.example.myapplication.model

object GetEpisode {
    const val URL = "https://api.bilicombili./pgc/player/web/playurl"

    enum class EPISODE(val id: Int,  val comment: String) {
        CONAN(323733, "名侦探柯南（中配）")
    }
}