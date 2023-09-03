package com.example.lantian_front.framework


sealed class Result<DATA, E> {
    class OK<DATA, E>(val data: DATA) : Result<DATA, E>() {
        operator fun invoke(): DATA = this.data
        operator fun <T> invoke(block: (data: DATA) -> T): T {
            return block(this.data)
        }
    }
    class Err<DATA, E>(val err: E) : Result<DATA, E>() {
        operator fun invoke(): E = this.err
        operator fun <T> invoke(block: (e: E) -> T): T {
            return block(this.err)
        }
    }

    fun onOk(block: (data: DATA) -> Unit): Result<DATA, E> {
        if (this is OK) {
            block(this.data)
        }
        return this
    }

    fun onErr(block: (err: E) -> Unit): Result<DATA, E> {
        if (this is Err) {
            block(this.err)
        }
        return this
    }

}

fun <T, Err> T.toOkResult() = Result.OK<T, Err>(this)

fun Boolean.toResult(): Result<Unit, Unit> = when (this) {
    true -> Result.OK(Unit)
    false -> Result.Err(Unit)
}