package com.example.myapplication.util

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.example.myapplication.nullCheck

object SPUtil {

    fun getSP(activity: Activity?): SharedPreferences? {
        return activity?.getPreferences(Context.MODE_PRIVATE).nullCheck("get SP")
    }

    fun putString(activity: Activity?, key: String, value: String) {
        getSP(activity)?.edit()?.let { editor ->
            editor.putString(key, value)
            editor.apply()
        }
    }

    fun getString(activity: Activity?, key: String): String? {
        return getSP(activity)?.getString(key, "")
    }

}