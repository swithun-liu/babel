package com.example.lantian_front.viewmodel.serversetting

import com.example.lantian_front.framework.BaseViewModel
import com.example.lantian_front.viewmodel.BusViewModel

sealed class Action: BaseViewModel.AAction() {
    class Init(val bus: BusViewModel, val lastTimeConnectServerIp: String?): Action()
    class SearchServer(val lanIp: String) : Action()
    class ConnectServer(val ip: String): Action()
    object GetServerStorage: Action()

}