package com.example.lantian_front.viewmodel.serversetting

interface UIState {
    /** 搜索按钮的文字 */
    val searchServerBtnText: String
    /** 可用的服务器IP */
    val availableServerIPs: List<String>
    /** 上次连接的服务器IP */
    val lastTimeConnectServerIp: String
    /** 当前连接的服务器IP */
    val currentConnectServerIp: String
}