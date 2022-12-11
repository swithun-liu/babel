package com.example.myapplication.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.MainActivity
import com.example.myapplication.swithunLog
import com.example.myapplication.util.*
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class VideoViewModel(private val activity: () -> MainActivity?) : ViewModel() {

    var qrCodeImage: ImageBitmap by mutableStateOf(ImageBitmap(100, 100))
    var loginStatus by mutableStateOf("未登陆")

    private val BILIBILI_LOGIN_QR_CODE_URL =
        "http://passport.bilibili.com/x/passport-login/web/qrcode/generate"
    private val BILIBILI_LOGIN_IN_URL =
        "http://passport.bilibili.com/x/passport-login/web/qrcode/poll"
    private val BILIBILI_MY_INFO_URL = "http://api.bilibili.com/x/space/myinfo"
    private val TAG = "swithun {VideoViewModel}"

    init {
        begin()
    }

    private fun begin() {
        viewModelScope.launch(Dispatchers.IO) {
            val suc = getCheckMyProfile()
            if (!suc) {
                qrCodeLogin()
                getCheckMyProfile()
            }
        }
    }

    @SuppressLint("LongLogTag")
    suspend fun qrCodeLogin() {
        Log.d(TAG, "qrCodeLogin")
        val qrCodeResponse = getRequest(BILIBILI_LOGIN_QR_CODE_URL) ?: return
        Log.d(TAG, "${qrCodeResponse.safeGetJSONObject("data")}")
        var qrcodeKey: String? = null
        qrCodeResponse.safeGetJSONObject("data")?.let { data ->
            data.safeGetString("url")?.let { it ->
                swithunLog("loginEnsureQrCodeUrl $it")
                val bitmap = BarcodeEncoder().encodeBitmap(it, BarcodeFormat.QR_CODE, 400, 400)
                    .asImageBitmap()
                qrCodeImage = bitmap
            }
            data.safeGetString("qrcode_key")?.let {
                qrcodeKey = it
            }
        }
        swithunLog("-")
        if (qrcodeKey == null) {
            swithunLog("--")
            Log.e(TAG, "qrcodeKey is null")
            return
        }
        while (true) {
            swithunLog("---")
            delay(300)
            val param = mutableMapOf<String, String>().apply {
                put("qrcode_key", qrcodeKey!!)
            }
            val response = getRequestWithOriginalResponse(BILIBILI_LOGIN_IN_URL, param) ?: let {
                Log.e(TAG, "(qrCodeLogin) -  response is null")
                return
            }
            val body = response.getRequestBodyJsonObject() ?: let {
                Log.e(TAG, "(qrCodeLogin) - body is null")
                return
            }
            val code = body.safeGetJSONObject("data")?.safeGetString("code") ?: let {
                Log.e(TAG, "(qrCodeLogin) - code is null")
                return
            }
            when (code) {
                "0" -> {
                    Log.i(TAG, "has confirm login")
                    val cookies = response.getRequestCookies()
                    val sharedPref =
                        activity?.invoke()?.getPreferences(Context.MODE_PRIVATE) ?: let {
                            Log.e(TAG, "get sharedPref failed")
                            return
                        }
                    with(sharedPref.edit()) {
                        for (cookie in cookies) {
                            val cookieFirstPart = cookie.split(";").getOrNull(0) ?: let {
                                Log.e(TAG, "cookie get first part failed")
                                return
                            }
                            val cookieKeyValue = cookieFirstPart.split("=")
                            if (cookieKeyValue.size != 2) {
                                Log.e(TAG, "cookie first part key value parse error")
                                return
                            } else {
                                val key = cookieKeyValue[0]
                                val value = cookieKeyValue[1]

                                Log.i(TAG, "")
                                putString(key, value)
                            }
                        }
                        apply()
                    }
                    break
                }
            }
        }
    }

    @SuppressLint("LongLogTag")
     suspend fun getCheckMyProfile(): Boolean {
        Log.d(TAG, "getCheckMyProfile activity: ${activity.invoke()}")
        val sharedPref = activity.invoke()?.getPreferences(Context.MODE_PRIVATE) ?: let {
            Log.e(TAG, "get sharedPref failed")
            return false
        }

        val cookieSessionData = sharedPref.getString("SESSDATA", "")

        if (cookieSessionData.isNullOrBlank()) {
            Log.e(TAG, "(getCheckMyProfile) cookieSessionData.isNullOrBlank()")
            return false
        } else {
            Log.i(TAG, "(getCheckMyProfile)  cookieSessionData: $cookieSessionData")
        }

        val param = mutableMapOf<String, String>().apply {
            put("Cookie", "SESSDATA=$cookieSessionData")
        }
        val response = getRequest(BILIBILI_MY_INFO_URL, headerParam = param) ?: let {
            Log.e(TAG, "(getCheckMyProfile) response is null")
            return false
        }

        response.safeGetString("code")?.let { code ->
            return when (code) {
                "0" -> {
                    Log.i(TAG, "(getCheckMyProfile) suc")
                    loginStatus = "已登陆"
                    true
                }
                else -> {
                    loginStatus = "登陆失败"
                    false
                }
            }
        }
        return false

    }
}