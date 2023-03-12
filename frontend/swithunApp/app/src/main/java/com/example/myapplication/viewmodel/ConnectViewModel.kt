package com.example.myapplication.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.ActivityVar
import com.example.myapplication.SwithunLog
import com.example.myapplication.code.OptionCode
import com.example.myapplication.model.KernelAndFrontEndJson
import com.example.myapplication.model.KernelConfig
import com.example.myapplication.websocket.RawData
import com.example.myapplication.websocket.WebSocketRepository
import com.google.gson.Gson
import com.swithun.liu.ServerSDK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.lang.Exception

class ConnectViewModel: ViewModel() {

    private var remoteWordFlow: Flow<RawData>? = null
    private val repository = WebSocketRepository()
    private var activityVar: ActivityVar? = null

    fun init(activityVar: ActivityVar) {
        this.activityVar = activityVar
    }

    fun create() {
        val kernelConfig = activityVar?.kernelConfig ?: return

        remoteWordFlow = repository.webSocketCreate(
            "http://${kernelConfig.kernelHost}/${KernelConfig.KernelPath.ConnectPath.connect}",
            viewModelScope,
            "Connect"
        )
        viewModelScope.launch(Dispatchers.IO) {
            remoteWordFlow?.collect {
                val json = it.json
                val gson = Gson()
                try {
                    SwithunLog.d(json)
                    val jsonObject = gson.fromJson(json, KernelAndFrontEndJson::class.java)
                    SwithunLog.d("get kernal code: ${jsonObject.code}, ${jsonObject.uuid}, ${jsonObject.content}")
                    handleCommand(jsonObject)
                } catch (e: Exception) {
                    SwithunLog.d("parse err")
                }

            }
        }
    }

    fun sendCommand(data: RawData) {
        repository.webSocketSend(data)
    }

    private fun responsePathList(uuid: String, code: Int, pathList: List<LocalPathItem>) {
        val gson = Gson()

        val jsonStr = gson.toJson(pathList)

        val jsonObject = KernelAndFrontEndJson(
            uuid = uuid,
            code = code,
            content = jsonStr
        )

        val jsonObjectStr = gson.toJson(jsonObject)

        sendCommand(RawData(jsonObjectStr))
    }


    private fun handleCommand(data: KernelAndFrontEndJson) {
        when (OptionCode.fromValue(data.code)) {
            OptionCode.GET_BASE_PATH_LIST_REQUEST -> {
                activityVar?.let {
                    it.fileManagerViewModel.viewModelScope.launch(Dispatchers.IO) {
                        val basePathList = it.fileManagerViewModel.getBasePathListFromLocal()
                        responsePathList(data.uuid, OptionCode.GET_BASE_PATH_LIST_RESPONSE.code, basePathList)
                    }
                }
            }
            OptionCode.GET_CHILDREN_PATH_LIST_REQUEST -> {
                activityVar?.let {
                    it.fileManagerViewModel.viewModelScope.launch(Dispatchers.IO) {
                        val childrenPathList = it.fileManagerViewModel.getChildrenPathListFromLocal(data.content)
                        responsePathList(data.uuid, OptionCode.GET_CHILDREN_PATH_LIST_RESPONSE.code, childrenPathList)
                    }
                }
            }
            null -> {
                SwithunLog.d("unKnown code")
            }
        }
   }
}