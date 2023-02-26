package com.example.myapplication.code

enum class OptionCode(val code: Int) {
    GET_BASE_PATH_LIST_REQUEST(1),
    GET_BASE_PATH_LIST_RESPONSE(2);

    companion object {
        fun fromValue(code: Int): OptionCode? = values().find { it.code == code }
    }
}