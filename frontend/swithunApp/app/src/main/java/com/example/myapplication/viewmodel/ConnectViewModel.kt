package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.websocket.RawData
import com.example.myapplication.websocket.WebSocketRepository
import kotlinx.coroutines.flow.Flow

class ConnectViewModel: ViewModel() {

    private var remoteWordFlow: Flow<RawData>? = null
    private val repository = WebSocketRepository()

    fun create() {
        remoteWordFlow = repository.webSocketCreate(
            "http://192.168.0.109:8088/connect",
            viewModelScope,
            "Connect"
        )
    }
}