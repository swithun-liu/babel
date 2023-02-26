package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.ActivityVar
import com.example.myapplication.SwithunLog
import com.example.myapplication.code.OptionCode
import com.example.myapplication.model.KernelAndFrontEndJson
import com.example.myapplication.websocket.RawData
import com.example.myapplication.websocket.WebSocketRepository
import com.google.gson.Gson
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
        remoteWordFlow = repository.webSocketCreate(
            "http://192.168.0.109:8088/connect",
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

    private fun handleCommand(data: KernelAndFrontEndJson) {
        when (OptionCode.fromValue(data.code)) {
            OptionCode.GET_BASE_PATH_LIST_REQUEST -> {
                activityVar?.let {
                    it.fileManagerViewModel.viewModelScope.launch(Dispatchers.IO) {
                        val basePathList = it.fileManagerViewModel.getBasePathListFromLocal()
                        val gson = Gson()

                        val jsonStr = gson.toJson(basePathList)

                        val jsonObject = KernelAndFrontEndJson(
                            uuid = data.uuid,
                            code = OptionCode.GET_BASE_PATH_LIST_RESPONSE.code,
                            content = jsonStr
                        )

                        val jsonObjectStr = gson.toJson(jsonObject)

                        sendCommand(RawData(jsonObjectStr))
                    }
                }
            }
            null -> {
                SwithunLog.d("unKnown code")
            }
        }
   }
}