package com.example.myapplication

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.websocket.RawData
import com.example.myapplication.websocket.WebSocketRepository
import java.lang.Exception
import kotlin.math.roundToInt

class MainViewModel: ViewModel() {

    var isFahrenheit by mutableStateOf(true)
    var result by mutableStateOf("")
    private val repository = WebSocketRepository()

    init {
        repository.webSocketCreate(viewModelScope)
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