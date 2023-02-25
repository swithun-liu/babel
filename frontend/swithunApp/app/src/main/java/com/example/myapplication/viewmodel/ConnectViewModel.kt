package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.SwithunLog
import com.example.myapplication.model.KernelConnectJson
import com.example.myapplication.websocket.RawData
import com.example.myapplication.websocket.WebSocketRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.lang.Error
import java.lang.Exception

class ConnectViewModel: ViewModel() {

    private var remoteWordFlow: Flow<RawData>? = null
    private val repository = WebSocketRepository()

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
                    val jsonObject = gson.fromJson(json, KernelConnectJson::class.java)
                     SwithunLog.d("get kernal code: ${jsonObject.code}, ${jsonObject.uuid}, ${jsonObject.content}")

                } catch (e: Exception) {
                    SwithunLog.d("parse err")
                }

            }
        }
    }

    fun sendCommand(data: RawData) {
        repository.webSocketSend(data)
    }
}