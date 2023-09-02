package com.example.lantian_front.websocket

import android.util.Log
import com.example.lantian_front.SwithunLog
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import okhttp3.*
import okio.ByteString


/** 封装给websocket发送的数据 */
sealed class RawDataBase {
    /** 纯文本 **/
    data class RawTextData(val json: String): RawDataBase()
    /** 二进制 **/
    data class RawByteData(val bytes: ByteString): RawDataBase()
}

interface IWebSocketChannel {
    fun getIncoming(): Flow<RawDataBase>
    fun isClosed(): Boolean
    fun close(
        code: Int = 1000,
        reason: String? = null
    )
    fun send(data: RawDataBase)

    suspend fun suspendSend(data: RawDataBase)
}

class WebSocketChannel(url: String, private val scope: CoroutineScope, private val tag: String): IWebSocketChannel {

    private var socket: WebSocket? = null
    private val incoming = Channel<RawDataBase>()
    private val outgoing = Channel<RawDataBase>()
    private val incomingFlow: Flow<RawDataBase> = incoming.consumeAsFlow()

    init {
        SwithunLog.d("WebSocketChannel[$tag] init")
        val okHttpClient = OkHttpClient.Builder().build()
        val request = Request.Builder()
            .url(url)
            .build()

        socket = okHttpClient.newWebSocket(request, WebSocketChannelListener(incoming, outgoing))

        okHttpClient.dispatcher.executorService.shutdown()

        scope.launch(Dispatchers.IO) {
            try {
                outgoing.consumeEach {
                    when(it) {
                        is RawDataBase.RawByteData -> {
                            SwithunLog.d("ws send # raw bytes")
                            socket?.send(it.bytes)
                        }
                        is RawDataBase.RawTextData -> {
                            SwithunLog.d("ws send # raw text")
                            socket?.send(it.json)
                        }
                    }
                    SwithunLog.d("ws send # end")
                    delay(20L)
                }
            } catch (e: Exception) {
                val eJson = Gson().toJson(e)
                SwithunLog.e("ws socket channel: cache: $eJson")
            } finally {
                SwithunLog.e("ws socket channel: finally")
            }
        }
    }

    override fun getIncoming(): Flow<RawDataBase> {
        return incomingFlow
    }

    override fun isClosed(): Boolean {
        return incoming.isClosedForReceive || outgoing.isClosedForSend
    }

    override fun close(code: Int, reason: String?) {
    }

    override fun send(data: RawDataBase) {
        scope.launch(Dispatchers.IO) {
            SwithunLog.d("[$tag] outgoing send data $data")
            outgoing.send(data)
        }
    }

    override suspend fun suspendSend(data: RawDataBase) {
        SwithunLog.d("[$tag] outgoing suspend send data $data")
        outgoing.send(data)
    }

    inner class WebSocketChannelListener(
        private val incoming: Channel<RawDataBase>,
        private val outgoing: Channel<RawDataBase>
    ): WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            SwithunLog.d("[WebSocketChannelListener[$tag]] - onOpen")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            SwithunLog.d("[WebSocketChannelListener[$tag]] - onMessage text $text")
            scope.launch(Dispatchers.IO) {
                SwithunLog.d("[WebSocketChannelListener[$tag]] - incoming send data")
                incoming.send(RawDataBase.RawTextData(text))
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            SwithunLog.d("[WebSocketChannelListener[$tag]] - onMessage bytes")
            scope.launch(Dispatchers.IO) {
                Log.d("swithun-xxxx", "incoming send data")
                incoming.send(RawDataBase.RawByteData(bytes))
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
            Log.d("swithun-xxxx", "[WebSocketChannelListener] - onFailure : ${t}")
            //SwithunLog.d(response)
            incoming.close()
            outgoing.close()
        }
    }
}
