package com.example.myapplication.model

object ServerConfig {

    var serverIP = "192.168.0.108"
    var serverPort = "8088"
    val serverHost get() = "$serverIP:$serverPort"

    object ServerPath {
        object GetPathList {
            const val path = "get_path_list"
            const val paramPath = "path"
        }
        object GetVideoPath {
            const val path = "get-video"
            const val paramPath = "path"
        }
        object WSPath {
            const val path = "ws"
        }
    }

}