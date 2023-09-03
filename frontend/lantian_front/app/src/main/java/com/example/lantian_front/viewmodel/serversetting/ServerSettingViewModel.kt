package com.example.lantian_front.viewmodel.serversetting

import androidx.lifecycle.viewModelScope
import com.example.lantian_front.SwithunLog
import com.example.lantian_front.framework.BaseViewModel
import com.example.lantian_front.framework.Result
import com.example.lantian_front.model.toTextRes
import com.example.lantian_front.viewmodel.BusViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ServerSettingViewModel: BaseViewModel<Action, UIState, MutableUIState>() {

    private var bus: BusViewModel? = null
    private var worker: Worker = Worker(viewModelScope)

    override fun reduce(action: Action) {
        when (action) {
            is Action.InitBus -> initBus(action)
            is Action.ConnectServer -> connectServer(action)
            is Action.SearchServer -> searchServer(action)
        }
    }

    private fun searchServer(action: Action.SearchServer) {
        innerUISate.searchServerBtnText = "正在搜索"

        viewModelScope.launch(Dispatchers.IO) {
            SwithunLog.d("searchServer", "begin")

            val toast = when (val result = worker.searchServer(action)) {

                is Result.Err -> result {
                    BusViewModel.Action.ToastAction("检索失败".toTextRes())
                }

                is Result.OK -> result { ips ->
                    innerUISate.availableServerIPs = ips.toList()
                    BusViewModel.Action.ToastAction("搜索到${ips.size}个服务器".toTextRes())
                }

            }

            innerUISate.searchServerBtnText = "LAN中搜索可用server"
            toast(toast)


            SwithunLog.d("searchServer", "end")
        }
    }

    private fun toast(action: BusViewModel.Action.ToastAction) {
        bus?.reduce(action)
    }

    private fun initBus(action: Action.InitBus) {
        this.bus = action.bus
    }

    private fun connectServer(action: Action.ConnectServer) {

    }

    override fun getInitialUIState(): MutableUIState {
        return MutableUIState()
    }

    companion object {
        private const val TAG = "ServerSettingViewModel"
    }
}