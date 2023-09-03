package com.example.lantian_front.viewmodel.serversetting

interface UIState {
    val searchServerBtnText: String
    val availableServerIPs: List<String>
    val lastTimeConnectServerIp: String
}