package com.example.myapplication

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.util.postRequest
import com.example.myapplication.util.safeGetString
import com.example.myapplication.websocket.RawData
import com.example.myapplication.websocket.WebSocketRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


@SuppressLint("LongLogTag")
class WordsViewModel: ViewModel() {

    private var remoteWordFlow: Flow<RawData>
    var wordsResult by mutableStateOf(WordsResult("", emptyList(), ""))
    private val repository = WebSocketRepository()

    private val YOUDAO_URL = "https://openapi.youdao.com/api"
    private val APP_KEY = "03f39bd127e854a5"
    private val APP_SECRET = "SQJK5f6cfEjHMdP6blgC8OCFlgsLQDCq"

    private val TAG = "swithun {WordsViewModel}"

    init {
        remoteWordFlow = repository.webSocketCreate(viewModelScope)

        viewModelScope.launch(Dispatchers.IO) {
            remoteWordFlow.collect {
                SwithunLog.d("remoteWordFlow collect ${it.json}")
                val q = it.json

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

                val jsonBody = postRequest(YOUDAO_URL, params) ?: return@collect

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
        }
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


    fun sendMessage(text: String) {
        repository.webSocketSend(RawData(text))
    }

}

data class WordsResult(
    val word: String,
    val explains: List<String>,
    val translation: String
)