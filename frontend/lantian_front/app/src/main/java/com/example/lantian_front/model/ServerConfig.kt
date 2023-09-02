package com.example.lantian_front.model

object ServerConfig {

    var serverIP = "192.168.0.108"
    var serverPort = "8088"
    val serverHost get() = "$serverIP:$serverPort"
    val serverPath = ServerPath

    const val fileChunkSize = 1000
    const val fileFrameSize = 1024

    object ServerPath {
        object GetPathList {
            const val path = "get_path_list"
            const val paramPath = "path"
        }
        object GetVideoPath {
            const val path = "get-video"
            const val paramPath = "path"
        }
        object WebSocketPath {
            const val path = "ws"
        }

        object TestPath {
            const val path = "test"
        }
    }

}