package com.swithun.lantian

sealed class Request {
    data class ConnectServer(
        val callback: Callback,
        val op: OptionCode = OptionCode.CONNECT_SERVER,
    ): Request()
    data class SearchServer(
        val subNet: String,
        val op: OptionCode = OptionCode.SEARCH_SERVER,
    ): Request() {
        class Response(
            val ips: Array<String>
        )
    }
}
