package com.example.myapplication.websocket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class WebSocketRepository {

    private var channel: IWebSocketChannel? = null

    fun webSocketCreate(url: String, scope: CoroutineScope, tag: String): Flow<RawData> {
        val flow: Flow<RawData>
        channel = WebSocketChannel(url, scope, tag).apply {
            flow = this.getIncoming()
        }
        return flow
    }

    fun webSocketSend(data: RawData): Boolean {
        return when (val c = channel) {
            null -> false
            else -> {
                c.send(data)
                true
            }
        }
    }
}