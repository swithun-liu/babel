package com.example.myapplication

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.websocket.RawData
import com.example.myapplication.websocket.WebSocketRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.lang.Exception
import kotlin.math.roundToInt

class MainViewModel: ViewModel() {

    var isFahrenheit by mutableStateOf(true)
    var result by mutableStateOf("")
    var remoteWord by mutableStateOf("")
    private lateinit var remoteWordFlow: Flow<RawData>
    private val repository = WebSocketRepository()

    init {
        remoteWordFlow = repository.webSocketCreate(viewModelScope)
        viewModelScope.launch {
            remoteWordFlow.collect {
                Log.d("swithun-xxxx", it.json)
                remoteWord = it.json
            }
        }
    }

    fun sendMessage(text: String) {
        repository.webSocketSend(RawData(text))
    }

    fun convertTemp(temp: String) {

        result = try {
            val tempInt = temp.toInt()

            if (isFahrenheit) {
                ((tempInt - 32) * 0.5556).roundToInt().toString()
            } else {
                ((tempInt * 1.8) + 32).roundToInt().toString()
            }
        } catch ( e: Exception ) {
            "Invalid Entry"
        }
    }

    fun switchChange() {
        isFahrenheit = !isFahrenheit
    }

}