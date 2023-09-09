package com.swithun.lantian

enum class OptionCode(val value: Long) {
    CONNECT_SERVER(1),
    SEARCH_SERVER(2),
    WS_TEXT_MSG(3),
    ;

    companion object {
        fun fromString(op: String) : OptionCode? {
            val opLong = try {
                op.toLong()
            } catch (e: Exception) {
                -1L
            }
            return fromLong(opLong)
        }

        fun fromLong(op: Long) : OptionCode? {
            return values().find { it.value == op }
        }
    }

}