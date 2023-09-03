package com.swithun.lantian

sealed interface Request<R> {
    fun <R> buildResponse(response: R): R = response

    data class ConnectServer(
        val serverIp: String,
        val callback: Callback = FrontEndSDK.callback,
        val op: OptionCode = OptionCode.CONNECT_SERVER,
    ) : Request<Response.ConnectServerRsp> {
    }

    data class SearchServer(
        val subNet: String,
        val op: OptionCode = OptionCode.SEARCH_SERVER,
    ) : Request<Response.SearchServerRsp>

    fun createResponse(response: R): R = response
}

sealed class Response {
    class ConnectServerRsp(val result: Boolean) : Response() {
    }

    class SearchServerRsp(
        val ips: Array<String>,
    ) : Response()
}
