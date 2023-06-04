package com.example.myapplication.model

import android.content.Context

sealed class TextRes {

    override fun toString(): String {
        return this.toString(null)
    }
    abstract fun toString(context: Context?) : String

    class StringText(val text: String) : TextRes() {

        override fun toString(context: Context?) : String {
            return text
        }

    }
}

fun String.toTextRes() : TextRes {
    return TextRes.StringText(this)
}