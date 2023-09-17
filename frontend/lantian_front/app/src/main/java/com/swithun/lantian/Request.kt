package com.swithun.lantian

import com.example.lantian_front.model.Storage

sealed interface Request<R> {
    fun <R> buildResponse(response: R): R = response

    class ConnectServer(
        val serverIp: String,
        val callback: JsonCallback,
        val op: OptionCode = OptionCode.CONNECT_SERVER,
    ) : Request<Response.ConnectServerRsp> {
    }

    class SearchServer(
        val subNet: String,
        val op: OptionCode = OptionCode.SEARCH_SERVER,
    ) : Request<Response.SearchServerRsp>

    class GetStorageList : Request<Response.GetStorageRsp>

    class GetFileOfStorage(
        val storage: Storage,
        val path: String,
    ) : Request<Response.GetFileOfStorageRsp>

    fun createResponse(response: R): R = response
}

sealed class Response {
    class ConnectServerRsp(val result: Boolean) : Response() {
    }

    class SearchServerRsp(
        val ips: Array<String>,
    ) : Response()

    class GetStorageRsp(
        val storages: Array<String>
    ) : Response()

    class GetFileOfStorageRsp(
        val fileList: String
    ) : Response()
}
