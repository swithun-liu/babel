package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.model.ActivityVar
import com.example.myapplication.SwithunLog
import com.example.myapplication.model.MessageTextDTO.OptionCode
import com.example.myapplication.model.MessageTextDTO
import com.example.myapplication.model.KernelConfig
import com.example.myapplication.websocket.RawDataBase
import com.example.myapplication.websocket.RawDataBase.RawTextData
import com.example.myapplication.websocket.WebSocketRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.lang.Exception

class ConnectKernelViewModel: ViewModel() {

    private var remoteWordFlow: Flow<RawDataBase>? = null
    private val repository = WebSocketRepository()
    private var activityVar: ActivityVar? = null

    fun init(activityVar: ActivityVar) {
        this.activityVar = activityVar
    }

     fun connectKernel() {
        val kernelConfig = activityVar?.kernelConfig ?: return

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
                            handleCommand(jsonObject)
                        } catch (e: Exception) {
                            SwithunLog.d("parse err")
                        }

                    }
                }
            }
        }
    }

    fun sendCommand(data: RawTextData) {
        repository.webSocketSend(data)
    }

    private fun responsePathList(uuid: String, code: Int, pathList: List<LocalPathItem>) {
        val gson = Gson()

        val jsonStr = gson.toJson(pathList)

        val jsonObject = MessageTextDTO(
            uuid = uuid,
            code = code,
            content = jsonStr,
            content_type = MessageTextDTO.ContentType.TEXT.type
        )

        val jsonObjectStr = gson.toJson(jsonObject)

        sendCommand(RawTextData(jsonObjectStr))
    }


    private fun handleCommand(data: MessageTextDTO) {
        when (OptionCode.fromValue(data.code)) {
            OptionCode.GET_BASE_PATH_LIST_REQUEST -> {
                activityVar?.let {
                    it.fileVM.viewModelScope.launch(Dispatchers.IO) {
                        val basePathList = it.fileVM.getBasePathListFromLocal()
                        responsePathList(data.uuid, OptionCode.GET_BASE_PATH_LIST_RESPONSE.code, basePathList)
                    }
                }
            }
            OptionCode.GET_CHILDREN_PATH_LIST_REQUEST -> {
                activityVar?.let {
                    it.fileVM.viewModelScope.launch(Dispatchers.IO) {
                        val childrenPathList = it.fileVM.getChildrenPathListFromLocal(data.content)
                        responsePathList(data.uuid, OptionCode.GET_CHILDREN_PATH_LIST_RESPONSE.code, childrenPathList)
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