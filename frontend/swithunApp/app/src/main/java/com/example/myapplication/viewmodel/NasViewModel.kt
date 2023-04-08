package com.example.myapplication.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.myapplication.ConnectServerViewModel
import com.example.myapplication.model.ActivityVar
import com.example.myapplication.MainActivity
import com.example.myapplication.model.MessageTextDTO
import com.example.myapplication.util.SystemUtil
import com.swithun.liu.ServerSDK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class NasViewModel(activity: () -> MainActivity) : BaseViewModel<NasViewModel.Action>() {

    // https://juejin.cn/post/6844903551408291848
    // https://github.com/koush/AndroidAsync
    private var activityVar: ActivityVar? = null

    var getAllServerBtnText: String by mutableStateOf("搜寻其他可用server")
    var allServersInLan: List<String> by mutableStateOf(mutableListOf())

    var startMeAsServerBtnText: String by mutableStateOf("启动server：未启动")

    fun init(activityVar: ActivityVar) {
        this.activityVar = activityVar
    }

    override fun reduce(action: Action) {
        when (action) {
            Action.ConnectMyServer -> connectMyServer()
            Action.StartMeAsServer -> startMeAsServer()
            is Action.DownloadTransferFile -> downloadTransferFile(action)
            Action.SearchAllServer -> searchAllServer()
        }
    }

    sealed class Action : BaseViewModel.Action() {
        object StartMeAsServer : Action()
        object ConnectMyServer : Action()
        object SearchAllServer : Action()
        class DownloadTransferFile(
            val text: String, val contentType: MessageTextDTO.ContentType, val context: Context,
        ) : Action()
    }

    private fun startMeAsServer() {
        viewModelScope.launch(Dispatchers.IO) {
            startMeAsServerBtnText = "启动server：启动中..."
            launch(Dispatchers.IO) {
                delay(1000)
                // 连接内核
                startMeAsServerBtnText = "启动server：连接内核中..."
                activityVar?.connectKernelVM?.reduce(ConnectKernelViewModel.Action.ConnectKernelAction)
                startMeAsServerBtnText = "启动server：已连接内核"
            }
            // 启动内核
            ServerSDK.startSever()
        }
    }

    private fun connectMyServer() {
        val connectServerM = activityVar?.connectServerVM ?: return
        val myIP = activityVar?.kernelConfig?.kernelIP ?: return
        connectServerM.reduce(ConnectServerViewModel.Action.ConnectServer(myIP))
    }

    private fun searchAllServer() {
        viewModelScope.launch(Dispatchers.IO) {
            getAllServerBtnText = "搜寻中..."

            val ips = ServerSDK.getAllServerInLAN()
            allServersInLan = ips.toList()

            getAllServerBtnText = "搜寻其他可用server"
        }
    }

    private fun downloadTransferFile(action: Action.DownloadTransferFile) {
        when (action.contentType) {
            MessageTextDTO.ContentType.TEXT -> {
                SystemUtil.pushText2Clipboard(action.context, action.text)
                viewModelScope.launch(Dispatchers.IO) {
                    activityVar?.scaffoldState?.showSnackbar(message = "已复制")
                }
            }
            MessageTextDTO.ContentType.IMAGE -> {
                activityVar?.connectServerVM?.reduce(ConnectServerViewModel.Action.GetSessionFile(action.text))
            }
        }
    }

}