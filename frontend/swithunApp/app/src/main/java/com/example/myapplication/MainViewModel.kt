package com.example.myapplication

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.websocket.RawData
import com.example.myapplication.websocket.WebSocketRepository
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import kotlin.math.roundToInt


class MainViewModel: ViewModel() {

    var isFahrenheit by mutableStateOf(true)
    var result by mutableStateOf("")
    private var remoteWordFlow: Flow<RawData>
    var wordsResult by mutableStateOf(WordsResult("", emptyList(), ""))
    private val repository = WebSocketRepository()

    private val YOUDAO_URL = "https://openapi.youdao.com/api"
    private val APP_KEY = "03f39bd127e854a5"
    private val APP_SECRET = "SQJK5f6cfEjHMdP6blgC8OCFlgsLQDCq"

    init {
        remoteWordFlow = repository.webSocketCreate(viewModelScope)
        viewModelScope.launch(Dispatchers.IO) {
            remoteWordFlow.collect {
                Log.d("swithun-xxxx", "1")
                Log.d("swithun-xxxx", it.json)
                Log.d("swithun-xxxx", "1")
                val q = it.json

                Log.d("swithun-xxxx", "1")

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
                Log.d("swithun-xxxx", "1")

                val body  = requestForHttp(YOUDAO_URL, params) ?: return@collect
                Log.d("swithun-xxxx", "1")

                Log.d("swithun-xxxx", body)

                val jsonBody = JSONObject(body)
                // translation
                var translation: String = ""
                val explains = mutableListOf<String>()
                if (jsonBody.has("translation")) {
                    jsonBody.getString("translation")?.let {
                        translation = it
                    }
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

                wordsResult = WordsResult(q, explains, translation)
            }
        }
    }

    private fun requestForHttp(my_url: String, params: Map<String, String>): String? {
        /** 创建HttpClient  */
        val httpClient = OkHttpClient.Builder().build()

        val formBody: RequestBody = FormBody.Builder().apply {
            for ((key, value) in params) {
                add(key, value)
            }
        }.build()

        val requestBody = Request.Builder()
            .url(my_url)
            .post(formBody)
            .build()

        val response = httpClient.newCall(requestBody).execute()

        return response.body?.string()

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
        var result: String
        return if (len <= 20) q else q.substring(0, 10) + len + q.substring(len - 10, len)
    }


    fun sendMessage(text: String) {
        repository.webSocketSend(RawData(text))
    }

    fun convertTemp(temp: String) {

        result = try {
            val tempInt = temp.toInt()

            if (isFahrenheit) {
                ((tempInt - 32) * 0.5556).roundToInt().toString()
            } else {
                ((tempInt * 1.8) + 32).roundToInt().toString()
            }
        } catch ( e: Exception ) {
            "Invalid Entry"
        }
    }

    fun switchChange() {
        isFahrenheit = !isFahrenheit
    }

}

data class WordsResult(
    val word: String,
    val explains: List<String>,
    val translation: String
)