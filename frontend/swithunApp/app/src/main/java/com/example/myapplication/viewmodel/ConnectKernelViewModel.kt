package com.example.myapplication.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.myapplication.Config
import com.example.myapplication.SwithunLog
import com.example.myapplication.framework.BaseViewModel
import com.example.myapplication.model.*
import com.example.myapplication.model.MessageTextDTO.OptionCode
import com.example.myapplication.websocket.RawDataBase
import com.example.myapplication.websocket.RawDataBase.RawTextData
import com.example.myapplication.websocket.WebSocketRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.lang.Exception

class ConnectKernelViewModel :
    BaseViewModel<ConnectKernelViewModel.Action, Unit, Unit>() {

    private var remoteWordFlow: Flow<RawDataBase>? = null
    private val repository = WebSocketRepository()
    private var vmCollection: VMCollection? = null

//    @Stable
//    interface UIState {
//    }
//
//    class MutableUIState: UIState {
//    }

    fun init(vmCollection: VMCollection) {
        this.vmCollection = vmCollection
    }


    override fun getInitialUIState(): Unit {
        return Unit
    }

    sealed class Action : BaseViewModel.Action() {
        object ConnectKernelAction : Action()
        class Response2Kernel(val dto: MessageDTO) : Action()
    }

    override fun reduce(action: Action) {
        when (action) {
            Action.ConnectKernelAction -> connectKernel()
            is Action.Response2Kernel -> response2Kernel(action)
        }
    }

    private fun connectKernel() {
        val kernelConfig = Config.kernelConfig

        remoteWordFlow = repository.webSocketCreate(
            "http://${kernelConfig.kernelHost}/${KernelConfig.KernelPath.ConnectPath.connect}",
            viewModelScope,
            "Connect"
        )
        viewModelScope.launch(Dispatchers.IO) {
            remoteWordFlow?.collect {
                when (it) {
                    is RawDataBase.RawByteData -> {

                    }
                    is RawTextData -> {
                        val json = it.json
                        val gson = Gson()
                        try {
                            SwithunLog.d("from kernel: $json")
                            val jsonObject = gson.fromJson(json, MessageTextDTO::class.java)
                            SwithunLog.d("get kernal code: ${jsonObject.code}, ${jsonObject.uuid}, ${jsonObject.content}")
                            handleResponse(jsonObject)
                        } catch (e: Exception) {
                            SwithunLog.d("parse err")
                        }

                    }
                }
            }
        }
    }

    private fun response2Kernel(action: Action.Response2Kernel) {
        when (val dto = action.dto) {
            is MessageBinaryDTO -> {

            }
            is MessageTextDTO -> {
                webSocketSend(RawTextData(dto.toJsonStr()))
            }
        }
    }

    private fun webSocketSend(data: RawTextData) {
        repository.webSocketSend(data)
    }


    private fun handleResponse(data: MessageTextDTO) {
        when (OptionCode.fromValue(data.code)) {
            OptionCode.GET_BASE_PATH_LIST_REQUEST -> {
                vmCollection?.let {
                    it.fileVM.viewModelScope.launch(Dispatchers.IO) {
                        val basePathList = it.fileVM.getBasePathListFromLocal()
                        val gson = Gson()
                        val pathListJsonStr = gson.toJson(basePathList)
                        val dto = MessageTextDTO(
                            data.uuid,
                            OptionCode.GET_BASE_PATH_LIST_RESPONSE.code,
                            pathListJsonStr,
                            MessageTextDTO.ContentType.TEXT.type
                        )
                        reduce(Action.Response2Kernel(dto))
                    }
                }
            }
            OptionCode.GET_CHILDREN_PATH_LIST_REQUEST -> {
                vmCollection?.let {
                    it.fileVM.viewModelScope.launch(Dispatchers.IO) {
                        val childrenPathList = it.fileVM.getChildrenPathListFromLocal(data.content)
                        val gson = Gson()
                        val pathListJsonStr = gson.toJson(childrenPathList)
                        val dto = MessageTextDTO(
                            data.uuid,
                            OptionCode.GET_BASE_PATH_LIST_RESPONSE.code,
                            pathListJsonStr,
                            MessageTextDTO.ContentType.TEXT.type
                        )
                        reduce(Action.Response2Kernel(dto))
                    }
                }
            }
            null -> {
                SwithunLog.d("unKnown code")
            }
            else -> {
                SwithunLog.d("other code")
            }
        }
    }
}