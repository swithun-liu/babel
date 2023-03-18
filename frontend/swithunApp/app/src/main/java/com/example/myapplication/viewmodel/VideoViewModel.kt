package com.example.myapplication.viewmodel

import android.annotation.SuppressLint
import android.util.Log
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.SwithunLog
import com.example.myapplication.errcode.LogInErrCode
import com.example.myapplication.model.GetEpisode
import com.example.myapplication.model.GetEpisodeList
import com.example.myapplication.model.SectionItem
import com.example.myapplication.model.ServerConfig
import com.example.myapplication.nullCheck
import com.example.myapplication.util.*
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import tv.danmaku.ijk.media.player.IjkMediaPlayer

class VideoViewModel(private val activity: () -> ComponentActivity) : ViewModel() {

    var qrCodeImage: ImageBitmap by mutableStateOf(ImageBitmap(100, 100))
    var loginStatus by mutableStateOf("未登陆")
    var currentProcess  by mutableStateOf(0F)
    var itemList by mutableStateOf(mutableListOf<SectionItem>())
    var itemCursor = 0
    var player = IjkMediaPlayer()
    var beginJob: Job? = null
    var playJob: Job? = null

    fun getNewPlayer() = IjkMediaPlayer().also {
        this.player = it
    }


    private val BILIBILI_LOGIN_QR_CODE_URL =
        "http://passport.bilibili.com/x/passport-login/web/qrcode/generate"
    private val BILIBILI_LOGIN_IN_URL =
        "http://passport.bilibili.com/x/passport-login/web/qrcode/poll"
    private val BILIBILI_MY_INFO_URL = "http://api.bilibili.com/x/space/myinfo"
    private val TAG = "swithun {VideoViewModel}"

    init {
        beginJob = begin()
    }

    fun play(
        surfaceView: SurfaceView,
        conanUrl: String,
        headerParams: HeaderParams ? = null,
        onComplete: (() -> Unit)? = null
    ) {
        playJob?.cancel()
        playJob = viewModelScope.launch(Dispatchers.IO) {
            SwithunLog.d("运行playe")
            val player = getNewPlayer()

            player.reset()
            // user-agent 需要用这个设置，否则header里设置会出现2个 https://blog.csdn.net/xiaoduzi1991/article/details/121968386
            player.setOption(
                IjkMediaPlayer.OPT_CATEGORY_FORMAT,
                "user-agent",
                "Bilibili Freedoooooom/MarkII"
            )
            player.setOption(
                IjkMediaPlayer.OPT_CATEGORY_FORMAT,
                "reconnect",
                1
            )
            //重连模式，如果中途服务器断开了连接，让它重新连接,参考 https://github.com/Bilibili/ijkplayer/issues/445
            player.setOption(
                IjkMediaPlayer.OPT_CATEGORY_FORMAT,
                "dns_cache_clear",
                1
            )
            // 解决 Hit dns cache but connect fail hostname
            player.setOption(
                IjkMediaPlayer.OPT_CATEGORY_FORMAT,
                "protocol_whitelist",
                "async,cache,crypto,file,http,https,ijkhttphook,ijkinject,ijklivehook,ijklongurl,ijksegment,ijktcphook,pipe,rtp,tcp,tls,udp,ijkurlhook,data,ftp"
            )
            // 设置播放源
            if (headerParams == null) {
                player.dataSource = conanUrl
            } else {
                player.setDataSource(conanUrl, headerParams.params)
            }
            // 设置surface
            player.setSurface(surfaceView.holder.surface)

            player.setOnPreparedListener {
                SwithunLog.d("old prepared")
                player.seekTo(0)
            }

            onComplete?.let { nonNullOnComplete ->
                player.setOnCompletionListener {
                    nonNullOnComplete.invoke()
                }
            }


            player.prepareAsync()
            player.start()
        }
    }


    private fun begin(): Job {
        return viewModelScope.launch(Dispatchers.IO) {
            delay(500)
            val suc = getCheckMyProfile()
            if (!suc) {
                qrCodeLogin()
                getCheckMyProfile()
            }
            getConanList()
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
                            SPUtil.putString(activity.invoke(), key, value)
                        }
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
        val response = getRequest(BILIBILI_MY_INFO_URL, headerParams = headerParams).nullCheck("get my profile") ?: return false

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

    suspend fun getConanByEpId(epId: Long): String? {
        beginJob?.join()

        for (i in 0 until itemList.size) {
            if (itemList[i].id == epId) {
                return getConan(i)
            }
        }
        return null
    }

    suspend fun getConan(chosePos: Int? = null): String? {

        val pos =
            chosePos
                ?: SPUtil.Conan.getCurrentConan(activity()).nullCheck("get current conan pos")
                ?: 0

        itemCursor = pos

        val item = itemList.getOrNull(pos)
        val epId = item?.id ?: GetEpisode.EPISODE.CONAN.id

        val urlEncodeParams = UrlEncodeParams().apply { put("ep_id", epId.toString()) }

        val headerParams = HeaderParams().apply { setBilibiliCookie(activity()) }

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

    suspend fun getNextConan(): String? {
        val nextCursor = (itemCursor + 1) % 500
        return getConan(nextCursor)
    }

    suspend fun getConanList() {
        val urlEncodeParams = UrlEncodeParams().apply {
            put("season_id", GetEpisodeList.EPISODE.CONAN.season_id.toString())
        }
        val headerParams = HeaderParams().apply {
        }

        val conanList = getRequest(
            GetEpisodeList.URL,
            urlEncodeParams = urlEncodeParams,
            headerParams = headerParams
        )
        val result = conanList?.safeGetJSONObject("result").nullCheck("get result", false) ?: return
        val main_section = result.safeGetJSONObject("main_section").nullCheck("get main_section", true) ?: return
        val episodes = main_section.safeGetJsonArray("episodes").nullCheck("get episodes", true) ?: return

        val items = mutableListOf<SectionItem>()
        for (i in 0 until  episodes.length()) {
            val obj = episodes.getJSONObject(i)
            val shortTitle = obj.safeGetString("title").nullCheck("get shortTitle") ?: continue
            val longTitle = obj.safeGetString("long_title").nullCheck("get longTitle") ?: continue
            val id = obj.safeGetLong("id").nullCheck("get id") ?: continue

            val item = SectionItem(shortTitle, longTitle, id)
            items.add(item)
        }

        itemList = items
        SwithunLog.d("haha - 2")
    }


    fun testGetHttpMp4(): String {
        //return "http://${ftpVM.myIPStr}:54321/files"
        return "http://192.168.31.15:54321/files"
    }

    fun playVideo(path: String): String {
        return "http://${ServerConfig.serverHost}/${ServerConfig.ServerPath.GetVideoPath.path}?${ServerConfig.ServerPath.GetVideoPath.paramPath}=$path"
    }

}