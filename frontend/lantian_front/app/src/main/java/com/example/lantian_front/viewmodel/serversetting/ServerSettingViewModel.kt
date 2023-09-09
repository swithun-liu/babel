package com.example.lantian_front.viewmodel.serversetting

import androidx.lifecycle.viewModelScope
import com.example.lantian_front.SwithunLog
import com.example.lantian_front.framework.BaseViewModel
import com.example.lantian_front.framework.Result
import com.example.lantian_front.model.toTextRes
import com.example.lantian_front.viewmodel.BusViewModel
import com.swithun.lantian.JsonCallback
import com.swithun.lantian.OptionCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ServerSettingViewModel: BaseViewModel<Action, UIState, MutableUIState>() {

    private var bus: BusViewModel? = null
    private var repository: Repository = Repository(viewModelScope)

    override fun reduce(action: Action) {
        when (action) {
            is Action.InitBus -> initBus(action)
            is Action.ConnectServer -> connectServer(action)
            is Action.SearchServer -> searchServer(action)
            is Action.GetServerStorage -> getServerStorage(action)
        }
    }

    private fun toast(action: BusViewModel.Action.ToastAction) {
        bus?.reduce(action)
    }

    private fun initBus(action: Action.InitBus) {
        this.bus = action.bus
    }


    private fun searchServer(action: Action.SearchServer) {
        innerUISate.searchServerBtnText = "正在搜索"

        viewModelScope.launch(Dispatchers.IO) {
            SwithunLog.d("searchServer", "begin")

            val toast = when (val result = repository.searchServer(action)) {

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
    private fun connectServer(action: Action.ConnectServer) {
        viewModelScope.launch(Dispatchers.IO) {
            SwithunLog.d(TAG, "begin")

            val jsonCallback = object : JsonCallback {

                override fun result(op: String, json: String) {
                    when (OptionCode.fromString(op)) {
                        OptionCode.CONNECT_SERVER -> {
                            // json: { "result": true } 或者  { "result": false } 解析
                            val suc = json.contains("true")

                            val toast = if (suc) {
                                innerUISate.currentConnectServerIp = action.ip
                                BusViewModel.Action.ToastAction("连接成功".toTextRes())
                            } else {
                                BusViewModel.Action.ToastAction("连接失败".toTextRes())
                            }

                            toast(toast)
                        }
                        OptionCode.SEARCH_SERVER -> { }
                        OptionCode.WS_TEXT_MSG -> { }
                        null -> { SwithunLog.e("unknown op code") }
                    }
                }

            }

            repository.connectServer(action, jsonCallback)

            SwithunLog.d(TAG, "end")
        }
    }

    private fun getServerStorage(action: Action.GetServerStorage) {
        viewModelScope.launch(Dispatchers.IO) {
            val storages = repository.getServerStorage()
        }
    }

    override fun getInitialUIState(): MutableUIState {
        return MutableUIState()
    }

    companion object {
        private const val TAG = "ServerSettingViewModel"
    }
}