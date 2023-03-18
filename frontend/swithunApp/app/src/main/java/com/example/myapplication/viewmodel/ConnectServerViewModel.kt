package com.example.myapplication

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.model.MessageDTO
import com.example.myapplication.model.ServerConfig
import com.example.myapplication.model.TransferData
import com.example.myapplication.util.postRequest
import com.example.myapplication.util.safeGetString
import com.example.myapplication.viewmodel.TransferBiz
import com.example.myapplication.websocket.RawData
import com.example.myapplication.websocket.WebSocketRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


@SuppressLint("LongLogTag")
class ConnectServerViewModel: ViewModel() {

    private var remoteWordFlow: Flow<RawData>? = null
    var wordsResult by mutableStateOf(WordsResult("", emptyList(), ""))
    private val repository = WebSocketRepository()

    private val YOUDAO_URL = "https://openapi.youdao.com/api"
    private val APP_KEY = "03f39bd127e854a5"
    private val APP_SECRET = "SQJK5f6cfEjHMdP6blgC8OCFlgsLQDCq"

    private val TAG = "swithun {WordsViewModel}"

    var activityVar: ActivityVar? = null

    var receivedData by mutableStateOf(mutableListOf<TransferData>())

    fun init(activityVar: ActivityVar) {
        this.activityVar = activityVar
    }

    fun connectServer(serverIp: String) {
        this.activityVar?.let { nonNollActivityVar ->

            nonNollActivityVar.serverConfig.serverIP = serverIp

            remoteWordFlow = repository.webSocketCreate(
                "http://${ServerConfig.serverHost}/${ServerConfig.ServerPath.WebSocketPath.path}",
                viewModelScope,
                "Client"
            )

            viewModelScope.launch(Dispatchers.IO) {
                remoteWordFlow?.collect {
                    SwithunLog.d("remoteWordFlow collect ${it.json}")
                    val gson = Gson()
                    try {
                        val message = gson.fromJson(it.json, MessageDTO::class.java)
                        when (MessageDTO.OptionCode.fromValue(message.code)) {
                            MessageDTO.OptionCode.TRANSFER_DATA -> {
                                val list = receivedData
                                val newList = mutableListOf<TransferData>(
                                    TransferData.TextData(message.content)
                                )
                                var i = 0
                                for (item in list) {
                                    i++
                                    if (i == 5) break
                                    newList.add(item)
                                }
                                receivedData = newList
                            }
                            null -> {
                                // activityVar?.scaffoldState?.snackbarHostState?.showSnackbar(
/*
                                    message = "无code"
                                )
*/
                            }
                        }
                    } catch (e: Exception) {
                        // activityVar?.scaffoldState?.snackbarHostState?.showSnackbar(
//                            message = "解析失败"
//                        )
                    }
                }
            }
        }
    }

    suspend fun translate(data: RawData) {
        val q = data.json

        val params = mutableMapOf<String, String>().apply {
            put("from", "en")
            put("to", "zh-CHS")
            put("signType", "v3")
            val curtime = (System.currentTimeMillis() / 1000).toString()
            put("curtime", curtime)
            put("appKey", APP_KEY)
            put("q", q)
            val salt = System.currentTimeMillis().toString()
            put("salt", salt)
            val signStr = "$APP_KEY${truncate(q)}$salt$curtime$APP_SECRET"
            val sign = getDigest(signStr)
            put("sign", sign ?: "")
        }

        val jsonBody = postRequest(YOUDAO_URL, params) ?: return

        SwithunLog.d(jsonBody.toString())

        // translation
        var translation: String = ""
        val explains = mutableListOf<String>()
        jsonBody.safeGetString("translation")?.let {
            translation = it
            Log.i(TAG, "translation: $it")
        }
        // explains
        if (jsonBody.has("basic")) {
            val basic= jsonBody.getJSONObject("basic")
            if (basic.has("explains")) {
                val a = basic.getJSONArray("explains")
                for (i in 0 until a.length()) {
                    a.get(i)?.toString()?.let {
                        explains.add(it)
                    }
                }
            } else {
                Log.d("swithun-xxxx", "no explains")
            }
        } else {
            Log.d("swithun-xxxx", "no basic")
        }

        delay(500)

        wordsResult = WordsResult(q, explains, translation)
    }

    /**
     * 生成加密字段
     */
    fun getDigest(string: String?): String? {
        if (string == null) {
            return null
        }
        val hexDigits = charArrayOf( '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' )
        val btInput: ByteArray = string.toByteArray(StandardCharsets.UTF_8)
        return try {
            val mdInst: MessageDigest = MessageDigest.getInstance("SHA-256")
            mdInst.update(btInput)
            val md: ByteArray = mdInst.digest()
            val j = md.size
            val str = CharArray(j * 2)
            var k = 0
            for (byte0: Byte in md) {
                str[k++] = hexDigits[byte0.toInt() shr 4 and 0xf]
                str[k++] = hexDigits[byte0.toInt() and 0xf]
            }

            String(str)
        } catch (e: NoSuchAlgorithmException) {
            null
        }
    }

    fun truncate(q: String?): String? {
        if (q == null) {
            return null
        }
        val len = q.length
        return if (len <= 20) q else q.substring(0, 10) + len + q.substring(len - 10, len)
    }

    fun transferData(data: String): Boolean {
        return repository.webSocketSend(RawData(TransferBiz.buildTransferData(data).toJsonStr()))
    }

    fun sendMessage(text: String) {
        repository.webSocketSend(RawData(text))
    }

    fun sendCommand(data: RawData) {
        repository.webSocketSend(data)
    }

}

data class WordsResult(
    val word: String,
    val explains: List<String>,
    val translation: String
)