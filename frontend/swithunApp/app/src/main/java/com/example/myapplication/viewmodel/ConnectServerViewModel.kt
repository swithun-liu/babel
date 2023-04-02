package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.model.ActivityVar
import com.example.myapplication.model.MessageDTO
import com.example.myapplication.model.ServerConfig
import com.example.myapplication.model.TransferData
import com.example.myapplication.util.postRequest
import com.example.myapplication.util.safeGetString
import com.example.myapplication.viewmodel.TransferBiz
import com.example.myapplication.websocket.RawDataBase
import com.example.myapplication.websocket.RawDataBase.RawTextData
import com.example.myapplication.websocket.WebSocketRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import okio.ByteString
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.UUID


@SuppressLint("LongLogTag")
class ConnectServerViewModel : ViewModel() {

    private var remoteWordFlow: Flow<RawDataBase>? = null
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
                    when (it) {
                        is RawDataBase.RawByteData -> {
                            SwithunLog.d("remoteWordFlow collect RawByteData")
                        }
                        is RawTextData -> {
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
                                        activityVar?.scaffoldState?.showSnackbar(message = "无code")
                                    }
                                    else -> {
                                        activityVar?.scaffoldState?.showSnackbar(message = "other code 不处理")
                                    }
                                }
                            } catch (e: Exception) {
                                activityVar?.scaffoldState?.showSnackbar(message = "解析失败")
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun translate(data: RawTextData) {
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
            val basic = jsonBody.getJSONObject("basic")
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
        return repository.webSocketSend(RawTextData(TransferBiz.buildTransferData(data).toJsonStr()))
    }

    fun transferData(uri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bufferSize = 4 * 1024
                    val buffer = ByteArray(bufferSize)
                    var seq = 0
                    val contentId = UUID.randomUUID().toString()
                    while (true) {
                        SwithunLog.d("send $seq")
                        val bytesRead = inputStream.read(buffer)
                        if (bytesRead == -1) {
                            break
                        }
                        val payload = buffer.sliceArray(0 until bytesRead)
                        val message = MessageDTO(contentId, seq, ByteString.of(*payload))
                        val messageBytes = message.toByteArray()
                        repository.webSocketSend(RawDataBase.RawByteData(ByteString.of(*messageBytes)))
                        seq++
                    }
                }
            } catch (e: Exception) {
                SwithunLog.e("err : $e")
            }
        }
    }
}

data class WordsResult(
    val word: String,
    val explains: List<String>,
    val translation: String,
)

data class MessageDTO(
    val contentId: String,
    val seq: Int,
    val payload: ByteString
) {
    fun toByteArray(): ByteArray {
        SwithunLog.d("contentId: $contentId")
        val contentIdBytes: ByteArray = contentId.toByteArray(Charsets.UTF_8).copyOf(36)
        SwithunLog.d("contentIdBytes $contentIdBytes")
        val seqBytes: ByteArray = ByteBuffer.allocate(4).putInt(seq).array()
        val payloadBytes = payload.toByteArray()
        val totalLength = contentIdBytes.size + seqBytes.size + payloadBytes.size
        val result = ByteArray(totalLength)
        System.arraycopy(contentIdBytes, 0, result, 0, contentIdBytes.size)
        System.arraycopy(seqBytes, 0, result, contentIdBytes.size, seqBytes.size)
        System.arraycopy(payloadBytes, 0, result, contentIdBytes.size + seqBytes.size, payloadBytes.size)
        return result
    }
}