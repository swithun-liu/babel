package com.example.lantian_front.viewmodel.serversetting

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MutableUIState: UIState {
    override var searchServerBtnText: String by mutableStateOf("LAN中搜索可用server")
    override var availableServerIPs: List<String> by mutableStateOf(listOf())
    override var lastTimeConnectServerIp: String by mutableStateOf("")
}