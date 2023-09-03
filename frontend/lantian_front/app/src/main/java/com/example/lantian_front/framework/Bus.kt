package com.example.lantian_front.framework

class Bus {

    fun dispatchRequest(bus: BusRequest) {

    }

}


sealed interface BusRequest {
    class ShowToast(val message: String)
}
