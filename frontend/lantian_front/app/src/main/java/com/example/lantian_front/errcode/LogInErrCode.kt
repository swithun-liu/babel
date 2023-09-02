package com.example.lantian_front.errcode


enum class LogInErrCode(val code: Int, val meaning: String) {
    NOT_LOGIN(-101, "账号未登陆");

    companion object {
        fun fromValue(value: Int): LogInErrCode? {
            for (i in LogInErrCode.values()) {
                if (value == i.code) {
                    return LogInErrCode.NOT_LOGIN
                }
            }
            return null
        }
    }
}