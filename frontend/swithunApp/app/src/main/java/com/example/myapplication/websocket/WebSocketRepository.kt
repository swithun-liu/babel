package com.example.myapplication.websocket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class WebSocketRepository {

    private lateinit var channel: IWebSocketChannel

    fun webSocketCreate(url: String, scope: CoroutineScope, tag: String): Flow<RawData> {
        channel = WebSocketChannel(url, scope, tag)
        return channel.getIncoming()
    }

    fun webSocketSend(data: RawData) {
        channel.send(data)
    }
}