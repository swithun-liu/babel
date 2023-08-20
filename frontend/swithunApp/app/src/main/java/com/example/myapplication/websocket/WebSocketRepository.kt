package com.example.myapplication.websocket

import com.example.myapplication.SwithunLog
import com.example.myapplication.nullCheck
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class WebSocketRepository {

    private var channel: IWebSocketChannel? = null

    fun webSocketCreate(url: String, scope: CoroutineScope, tag: String): Flow<RawDataBase> {
        val flow: Flow<RawDataBase>
        channel = WebSocketChannel(url, scope, tag).apply {
            flow = this.getIncoming()
        }
        return flow
    }

    fun webSocketSend(data: RawDataBase): Boolean {
        return when (val c = channel) {
            null -> false
            else -> {
                c.send(data)
                true
            }
        }
    }

    suspend fun webSocketSuspendSend(data: RawDataBase): Boolean {
        return when (val c = channel) {
            null -> {
                SwithunLog.e("WebSocketRepository webSocketSuspendSend channel is null")
                false
            }
            else -> {
                SwithunLog.e("WebSocketRepository webSocketSuspendSend channel send ${data} ${c.isClosed()}")

                c.suspendSend(data)
                true
            }
        }
    }
}