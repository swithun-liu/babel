package com.example.myapplication.websocket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class WebSocketRepository {

    private lateinit var channel: IWebSocketChannel

    fun webSocketCreate(scope: CoroutineScope): Flow<RawData> {
        channel = WebSocketChannel(scope)
        return channel.getIncoming()
    }

    fun webSocketSend(data: RawData) {
        channel.send(data)
    }
}