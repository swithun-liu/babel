package com.example.lantian_front.viewmodel.serversetting

import com.example.lantian_front.framework.BaseViewModel

sealed class Event: BaseViewModel.AEvent {
    class UpdateLastTimeConnectServerIp(val ip: String): Event()
}