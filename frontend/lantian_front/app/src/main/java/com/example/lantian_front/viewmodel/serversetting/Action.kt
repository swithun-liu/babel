package com.example.lantian_front.viewmodel.serversetting

import com.example.lantian_front.framework.BaseViewModel
import com.example.lantian_front.viewmodel.BusViewModel

sealed class Action: BaseViewModel.AAction() {
    class InitBus(val bus: BusViewModel): Action()
    class SearchServer(val lanIp: String) : Action()
    class ConnectServer(val ip: String): Action()
    object GetServerStorage: Action()

}