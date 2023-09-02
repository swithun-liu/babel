package com.example.lantian_front.framework

sealed class Async<T> {
    class Uninitialized<T> : Async<T>()
    class Loading<T> : Async<T>()
    class Success<T>(val value: T) : Async<T>()
    data class Fail<T>(val err: ErrorMsg) : Async<T>() {
        data class ErrorMsg(val msg: String)
    }
}