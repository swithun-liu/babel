package com.example.myapplication.websocket

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import okhttp3.*
import okio.ByteString


data class RawData(val json: String)

interface IWebSocketChannel {
    fun getIncoming(): Flow<RawData>
    fun isClosed(): Boolean
    fun close(
        code: Int = 1000,
        reason: String? = null
    )
    fun send(data: RawData)
}

class WebSocketChannel(private val scope: CoroutineScope): IWebSocketChannel {

    private val socket: WebSocket
    private val incoming = Channel<RawData>()
    private val outgoing = Channel<RawData>()
    private val incomingFlow: Flow<RawData> = incoming.consumeAsFlow()

    init {
        Log.d("swithun-xxxx", "WebSocketChannel init")
        val okHttpClient = OkHttpClient.Builder().build()
        val request = Request.Builder()
            .url("http://192.168.0.103:8088/ws")
            .build()
        socket = okHttpClient.newWebSocket(request, WebSocketChannelListener(incoming, outgoing))

        okHttpClient.dispatcher.executorService.shutdown()

        scope.launch(Dispatchers.IO) {
            try {
                outgoing.consumeEach {
                    socket.send(it.json)
                }
            } finally {
                close()
            }
        }
    }

    override fun getIncoming(): Flow<RawData> {
        return incomingFlow
    }

    override fun isClosed(): Boolean {
        return incoming.isClosedForReceive || outgoing.isClosedForSend
    }

    override fun close(code: Int, reason: String?) {
    }

    override fun send(data: RawData) {
        scope.launch(Dispatchers.IO) {
            Log.d("swithun-xxxx", "outgoing send data")
            outgoing.send(data)
        }
    }

    inner class WebSocketChannelListener(
        private val incoming: Channel<RawData>,
        private val outgoing: Channel<RawData>
    ): WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d("swithun-xxxx", "[WebSocketChannelListener] - onOpen")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d("swithun-xxxx", "[WebSocketChannelListener] - onMessage text")
            scope.launch(Dispatchers.IO) {
                Log.d("swithun-xxxx", "incoming send data")
                incoming.send(RawData(text))
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            Log.d("swithun-xxxx", "[WebSocketChannelListener] - onMessage")
            scope.launch(Dispatchers.IO) {
                Log.d("swithun-xxxx", "incoming send data")
                incoming.send(RawData(bytes.toString()))
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d("swithun-xxxx", "[WebSocketChannelListener] - onClosing")
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d("swithun-xxxx", "[WebSocketChannelListener] - onClosed")
            incoming.close()
            outgoing.close()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.d("swithun-xxxx", "[WebSocketChannelListener] - onFailure")
            incoming.close(t)
            outgoing.close(t)
        }
    }
}
