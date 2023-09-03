package com.example.lantian_front.viewmodel.serversetting

import com.swithun.lantian.FrontEndSDK
import com.swithun.lantian.Request
import kotlinx.coroutines.CoroutineScope
import com.example.lantian_front.framework.Result
import com.example.lantian_front.framework.toOkResult

class Worker(viewModelScope: CoroutineScope) {

    fun searchServer(action: Action.SearchServer): Result<Array<String>, Unit> {
        val result = FrontEndSDK.request(Request.SearchServer(action.lanIp))
        val ips = (result as? Request.SearchServer.Response)?.ips ?: emptyArray()
        return ips.toOkResult()
    }

}