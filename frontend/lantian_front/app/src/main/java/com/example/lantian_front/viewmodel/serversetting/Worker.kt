package com.example.lantian_front.viewmodel.serversetting

import com.swithun.lantian.FrontEndSDK
import com.swithun.lantian.Request
import kotlinx.coroutines.CoroutineScope
import com.example.lantian_front.framework.Result
import com.example.lantian_front.framework.toOkResult
import com.example.lantian_front.framework.toResult

class Worker(viewModelScope: CoroutineScope) {

    fun searchServer(action: Action.SearchServer): Result<Array<String>, Unit> {
        val result = FrontEndSDK.request(Request.SearchServer(action.lanIp))

        val ips = result.ips
        return ips.toOkResult()
    }

    fun connectServer(action: Action.ConnectServer): Result<Unit, Unit> {

        val result = FrontEndSDK.request(Request.ConnectServer(action.ip))
        return result.result.toResult()

    }

}