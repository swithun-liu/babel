package com.example.lantian_front.model

object GetEpisode {
    const val URL = "https://api.bilibili.com/pgc/player/web/playurl"

    enum class EPISODE(val id: Int, val season_id: Int,  val comment: String) {
        CONAN(323843, 33415, "名侦探柯南（中配）")
    }
}

object GetEpisodeList {
    const val URL = "https://api.bilibili.com/pgc/web/season/section"

    enum class EPISODE(val season_id: Int, val comment: String) {
        CONAN(33415, "名侦探柯南（中配）")
    }
}