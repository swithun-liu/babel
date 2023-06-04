package com.example.myapplication.viewmodel

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.viewModelScope
import com.example.myapplication.SwithunLog
import com.example.myapplication.errcode.LogInErrCode
import com.example.myapplication.framework.BaseViewModel2
import com.example.myapplication.model.*
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

class VideoViewModel :
    BaseViewModel2<VideoViewModel.Action, VideoViewModel.VideoUIState, VideoViewModel.MutableVideoUIState>() {

    private var vmCollection: VMCollection? = null

    private var itemCursor = 0
    private var beginJob: Job? = null
    private var playJob: Job? = null

    override fun getInitialUIState(): MutableVideoUIState {
        return MutableVideoUIState()
    }

    @Stable
    interface VideoUIState {
        val currentProcess: Float
        var itemList: List<SectionItem>
        var loginStatus: String
        var aspectRatio: Float
        var qrCodeImage: ImageBitmap
        var player: IjkMediaPlayer
    }

    class MutableVideoUIState : VideoUIState {
        override var currentProcess: Float by mutableStateOf(0F)
        override var itemList: List<SectionItem> by mutableStateOf(listOf())
        override var loginStatus by mutableStateOf("未登陆")
        override var aspectRatio: Float by mutableStateOf(1.toFloat())
        override var qrCodeImage: ImageBitmap by mutableStateOf(ImageBitmap(100, 100))
        override var player: IjkMediaPlayer by mutableStateOf(IjkMediaPlayer())
    }

    fun init(vmCollection: VMCollection) {
        this.vmCollection = vmCollection
    }

    sealed class Action : BaseViewModel2.Action() {
        data class PlayVideoAction(
            val videoUrl: String,
            val headerParams: HeaderParams? = null,
            val onComplete: (() -> Unit)? = null,
        ) : Action()

        data class CyclePlayEpisode(
            val epId: Long,
        ) : Action()
    }

    override fun reduce(action: Action) {
        when (action) {
            is Action.PlayVideoAction -> playVideo(action)
            is Action.CyclePlayEpisode -> cyclePlayEpisodeByEpId(action.epId)
        }
    }

    private fun getNewPlayer() = IjkMediaPlayer().also {
        this.uiState.player = it
    }

    private val BILIBILI_LOGIN_QR_CODE_URL =
        "http://passport.bilibili.com/x/passport-login/web/qrcode/generate"
    private val BILIBILI_LOGIN_IN_URL =
        "http://passport.bilibili.com/x/passport-login/web/qrcode/poll"
    private val BILIBILI_MY_INFO_URL = "http://api.bilibili.com/x/space/myinfo"
    private val TAG = "swithun {VideoViewModel}"

    fun init() {
        beginJob = begin()
    }

    private fun updateCurrentVideoProcess(process: Float) {
        innerUISate.currentProcess = process
    }

    private fun playVideo(
        action: Action.PlayVideoAction,
    ) {

        val conanUrl: String = action.videoUrl
        val headerParams: HeaderParams? = action.headerParams
        val onComplete: (() -> Unit)? = action.onComplete


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

            player.setOnPreparedListener {
                val w = player.videoWidth.takeIf { it != 0 } ?: 1
                val h = player.videoHeight.takeIf { it != 0 } ?: 1
                innerUISate.aspectRatio = w.toFloat() / h
                SwithunLog.d("player ready w: $w h: $h aspectRatio: ${uiState.aspectRatio}")
                player.seekTo(0)
            }


            onComplete?.let { nonNullOnComplete ->
                player.setOnCompletionListener {
                    nonNullOnComplete.invoke()
                }
            }


            player.prepareAsync()
            player.start()

            while (true) {
                delay(500)
                val duration = when (val duration = player.duration) {
                    0L -> 1F
                    else -> duration.toFloat()
                }
                updateCurrentVideoProcess(player.currentPosition.toFloat() / duration)
            }
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
            delay(1000)
            getConanList()
        }
    }

    @SuppressLint("LongLogTag")
    private suspend fun qrCodeLogin() {
        SwithunLog.d("try qrCodeLogin")
        val qrCodeResponse = getRequest(BILIBILI_LOGIN_QR_CODE_URL) ?: return
        Log.d(TAG, "${qrCodeResponse.safeGetJSONObject("data")}")
        var qrcodeKey: String? = null
        qrCodeResponse.safeGetJSONObject("data")?.let { data ->
            data.safeGetString("url")?.let { it ->
                SwithunLog.d("loginEnsureQrCodeUrl $it")
                val bitmap = BarcodeEncoder().encodeBitmap(it, BarcodeFormat.QR_CODE, 400, 400)
                    .asImageBitmap()
                uiState.qrCodeImage = bitmap
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
            val response = getRequestWithOriginalResponse(
                BILIBILI_LOGIN_IN_URL,
                urlEncodeParams = urlEncodeParams
            ).nullCheck("response") ?: return
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
                            vmCollection?.shareViewModel?.reduce(ShareViewModel.Action.NeedActivity { activity ->
                                SPUtil.putString(activity, key, value)
                            })
                        }
                    }

                    break
                }
            }
        }
    }

    private suspend fun getCheckMyProfile(): Boolean {
        val activityVar = vmCollection ?: return false

        val headerParams = HeaderParams().apply {
            activityVar.shareViewModel.suspendReduce(ShareViewModel.Action.NeedActivity {
                setBilibiliCookie(it)
            })
        }
        val response = getRequest(
            BILIBILI_MY_INFO_URL,
            headerParams = headerParams
        ).nullCheck("get my profile") ?: return false

        response.safeGetString("code")?.let { code ->
            return when (code) {
                "0" -> {
                    SwithunLog.d("登陆成功")
                    innerUISate.loginStatus = "已登陆"
                    true
                }
                else -> {
                    SwithunLog.d("登陆失败 - errCode: $code: ${LogInErrCode.fromValue(code.toInt())}")
                    innerUISate.loginStatus = "登陆失败"
                    false
                }
            }
        }
        return false

    }

    private fun cyclePlayEpisodeByEpId(epId: Long) {
        var itemPos = 0
        for (i in 0 until innerUISate.itemList.size) {
            if (innerUISate.itemList[i].id == epId) {
                itemPos = i
                break
            }
        }

        cyclePlayEpisode(itemPos)
    }

    private fun cyclePlayEpisode(epPos: Int) {
        viewModelScope.launch(Dispatchers.IO) {

            itemCursor = epPos

            val epUrl =
                getConan(epPos).nullCheck("cyclePlayEpisode getConan", true) ?: return@launch

            val headerParams = HeaderParams().apply { setBilibiliReferer() }

            playVideo(Action.PlayVideoAction(
                epUrl,
                headerParams
            ) {
                viewModelScope.launch {
                    val nextEpPos = getNextEpisodePos()
                    cyclePlayEpisode(nextEpPos)
                }
            })
        }
    }


    private suspend fun getConan(chosePos: Int? = null): String? {
        val pos = when {
            chosePos != null -> chosePos
            else -> {
                var pos = 0
                vmCollection?.shareViewModel?.suspendReduce(ShareViewModel.Action.NeedActivity { activity ->
                    pos = SPUtil.Conan.getCurrentConan(activity) ?: 0
                })
                pos
            }
        }



        itemCursor = pos

        val item = innerUISate.itemList.getOrNull(pos)
        val epId = item?.id ?: GetEpisode.EPISODE.CONAN.id

        val urlEncodeParams = UrlEncodeParams().apply { put("ep_id", epId.toString()) }

        val headerParams = HeaderParams().apply {
            vmCollection?.shareViewModel?.suspendReduce(ShareViewModel.Action.NeedActivity { activity ->
                setBilibiliCookie(activity)
            })
        }

        val videoInfo = getRequest(
            GetEpisode.URL,
            urlEncodeParams = urlEncodeParams,
            headerParams = headerParams
        ).nullCheck("get videoInfo", true) ?: return null
        val result =
            videoInfo.safeGetJSONObject("result").nullCheck("get result", true) ?: return null
        val durl: JSONArray =
            result.safeGetJsonArray("durl").nullCheck("get durl", true) ?: return null
        val durl_0 = (if (durl.length() > 0) {
            durl.get(0)
        } else {
            null
        } as? JSONObject).nullCheck("get durl_0") ?: return null
        val durl_url = durl_0.safeGetString("url").nullCheck("get durl_url", true) ?: return null

        return durl_url
    }


    private fun getNextEpisodePos(): Int {
        return (itemCursor + 1) % 500
    }

    private suspend fun getConanList() {
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
        val main_section =
            result.safeGetJSONObject("main_section").nullCheck("get main_section", true) ?: return
        val episodes =
            main_section.safeGetJsonArray("episodes").nullCheck("get episodes", true) ?: return

        val items = mutableListOf<SectionItem>()
        for (i in 0 until episodes.length()) {
            val obj = episodes.getJSONObject(i)
            val shortTitle = obj.safeGetString("title").nullCheck("get shortTitle") ?: continue
            val longTitle = obj.safeGetString("long_title").nullCheck("get longTitle") ?: continue
            val id = obj.safeGetLong("id").nullCheck("get id") ?: continue

            val item = SectionItem(shortTitle, longTitle, id)
            items.add(item)
        }


        innerUISate.itemList = items
        SwithunLog.d("haha - 2")
    }

}
