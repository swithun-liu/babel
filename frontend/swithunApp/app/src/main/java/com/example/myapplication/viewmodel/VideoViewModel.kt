package com.example.myapplication.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.MainActivity
import com.example.myapplication.SwithunLog
import com.example.myapplication.errcode.LogInErrCode
import com.example.myapplication.model.GetEpisode
import com.example.myapplication.nullCheck
import com.example.myapplication.util.*
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.commons.lang3.StringEscapeUtils
import org.json.JSONArray
import org.json.JSONObject
import tv.danmaku.ijk.media.player.IjkMediaPlayer

class VideoViewModel(private val activity: () -> MainActivity?) : ViewModel() {

    var qrCodeImage: ImageBitmap by mutableStateOf(ImageBitmap(100, 100))
    var loginStatus by mutableStateOf("未登陆")
    val player = IjkMediaPlayer()

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
        SwithunLog.d("try qrCodeLogin")
        val qrCodeResponse = getRequest(BILIBILI_LOGIN_QR_CODE_URL) ?: return
        Log.d(TAG, "${qrCodeResponse.safeGetJSONObject("data")}")
        var qrcodeKey: String? = null
        qrCodeResponse.safeGetJSONObject("data")?.let { data ->
            data.safeGetString("url")?.let { it ->
                SwithunLog.d("loginEnsureQrCodeUrl $it")
                val bitmap = BarcodeEncoder().encodeBitmap(it, BarcodeFormat.QR_CODE, 400, 400)
                    .asImageBitmap()
                qrCodeImage = bitmap
            }
            data.safeGetString("qrcode_key")?.let {
                qrcodeKey = it
            }
        }
        if (qrcodeKey == null) {
            Log.e(TAG, "qrcodeKey is null")
            return
        }
        while (true) {
            delay(300)
            val urlEncodeParams = UrlEncodeParams().apply {
                put("qrcode_key", qrcodeKey!!)
            }
            val response = getRequestWithOriginalResponse(BILIBILI_LOGIN_IN_URL, urlEncodeParams = urlEncodeParams).nullCheck("response") ?: return
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
        val headerParams = HeaderParams().apply {
            setBilibiliCookie(activity.invoke())
        }
        val response = getRequest(BILIBILI_MY_INFO_URL, headerParams = headerParams) ?: let {
            Log.e(TAG, "(getCheckMyProfile) response is null")
            return false
        }

        delay(500)
        response.safeGetString("code")?.let { code ->
            return when (code) {
                "0" -> {
                    SwithunLog.d("登陆成功")
                    loginStatus = "已登陆"
                    true
                }
                else -> {
                    SwithunLog.d("登陆失败 - errCode: $code: ${LogInErrCode.fromValue(code.toInt())}")
                    loginStatus = "登陆失败"
                    false
                }
            }
        }
        return false

    }

    suspend fun getConan(): String? {
        val urlEncodeParams = UrlEncodeParams().apply {
            put("ep_id", GetEpisode.EPISODE.CONAN.id.toString())
        }
        val headerParams = HeaderParams().apply {
            setBilibiliCookie(activity())
        }
        val videoInfo = getRequest(
            GetEpisode.URL,
            urlEncodeParams = urlEncodeParams,
            headerParams = headerParams
        ).nullCheck("get videoInfo", true) ?: return null
        val result = videoInfo.safeGetJSONObject("result").nullCheck("get result", true) ?: return null
        val durl: JSONArray = result.safeGetJsonArray("durl").nullCheck("get durl", true) ?: return null
        val durl_0 = (if (durl.length() > 0) { durl.get(0) } else { null } as? JSONObject).nullCheck("get durl_0") ?: return null
        val durl_url = durl_0.safeGetString("url").nullCheck("get durl_url", true) ?: return null

        return durl_url
    }
}