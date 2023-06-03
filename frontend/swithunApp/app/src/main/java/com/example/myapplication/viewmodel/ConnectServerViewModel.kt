package com.example.myapplication

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.myapplication.model.*
import com.example.myapplication.util.postRequest
import com.example.myapplication.util.safeGetString
import com.example.myapplication.framework.BaseViewModel
import com.example.myapplication.viewmodel.NasViewModel
import com.example.myapplication.viewmodel.biz.TransferBiz
import com.example.myapplication.websocket.RawDataBase
import com.example.myapplication.websocket.RawDataBase.RawTextData
import com.example.myapplication.websocket.WebSocketRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import okio.ByteString
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.UUID


class ConnectServerViewModel : BaseViewModel<ConnectServerViewModel.Action>() {

    private var remoteWordFlow: Flow<RawDataBase>? = null
    var wordsResult by mutableStateOf(WordsResult("", emptyList(), ""))
    private val repository = WebSocketRepository()

    private val YOUDAO_URL = "https://openapi.youdao.com/api"
    private val APP_KEY = "03f39bd127e854a5"
    private val APP_SECRET = "SQJK5f6cfEjHMdP6blgC8OCFlgsLQDCq"

    private val TAG = "swithun{WordsViewModel"

    var vmCollection: VMCollection? = null

    var receivedData by mutableStateOf(listOf<TransferData>())

    // contentId - 文件名
    private var receivingFileMap = mutableMapOf<String, String>()

    fun init(vmCollection: VMCollection) {
        this.vmCollection = vmCollection
    }

    open class Action : BaseViewModel.Action() {
        class ConnectServer(val serverIp: String) : Action()
        class PostSessionText(val text: String) : Action()
        class PostSessionFile(val uri: Uri, val context: Context, val parentPath: String) : Action()
        class GetSessionFile(val fileName: String) : Action()
    }

    override fun reduce(action: Action) {
        when (action) {
            is Action.ConnectServer -> connectServer(action)
            is Action.PostSessionText -> postText(action)
            is Action.PostSessionFile -> postSessionFile(action)
            is Action.GetSessionFile -> getSessionFile(action)
        }
    }

    private fun connectServer(action: Action.ConnectServer) {

        val serverIp = action.serverIp

        val activityVar = this.vmCollection ?: return

        Config.serverConfig.serverIP = serverIp

        remoteWordFlow = repository.webSocketCreate(
            "http://${ServerConfig.serverHost}/${ServerConfig.ServerPath.WebSocketPath.path}",
            viewModelScope,
            "Client"
        )

        // 保存最后一次连接的服务器ip
        activityVar.nasVM.reduce(NasViewModel.Action.ChangeLastTimeConnectServer(serverIp))

        viewModelScope.launch(Dispatchers.IO) {
            remoteWordFlow?.collect {
                when (it) {
                    is RawDataBase.RawByteData -> {
                        SwithunLog.d("remoteWordFlow collect RawByteData")
                        viewModelScope.launch(Dispatchers.IO) {
                            handleByteData(it)
                        }
                    }
                    is RawTextData -> {
                        SwithunLog.d("remoteWordFlow collect RawTextData ${it.json}")
                        val gson = Gson()
                        try {
                            val message = gson.fromJson(it.json, MessageTextDTO::class.java)
                            when (MessageTextDTO.OptionCode.fromValue(message.code)) {
                                // 其他client发送到会话数据(文本/图片/文件)
                                MessageTextDTO.OptionCode.POST_SESSION_TEXT -> {
                                    val oodList = receivedData
                                    SwithunLog.d("oodList: $oodList")
                                    when (MessageTextDTO.ContentType.fromValue(message.content_type)) {
                                        null -> {
                                            activityVar.scaffoldState?.showSnackbar(message = "未知 内容类型")
                                            SwithunLog.d("unknown content_type")
                                        }
                                        MessageTextDTO.ContentType.TEXT -> {
                                            SwithunLog.d("content_type: Text")
                                            val uodList = mutableListOf<TransferData>(
                                                TransferData.TextData(message.content)
                                            )
                                            uodList.addAll(oodList.take(4))
                                            SwithunLog.d("uodList: $uodList")
                                            receivedData = uodList
                                        }
                                        MessageTextDTO.ContentType.IMAGE -> {
                                            SwithunLog.d("content_type: Image")
                                            val uodList = mutableListOf<TransferData>(
                                                TransferData.ImageData(message.content)
                                            )
                                            uodList.addAll(oodList.take(4))
                                            SwithunLog.d("uodList: $uodList")
                                            receivedData = uodList
                                        }
                                    }

                                }
                                null -> {
                                    activityVar.scaffoldState?.showSnackbar(message = "无code")
                                }
                                else -> {
                                    activityVar.scaffoldState?.showSnackbar(message = "other code 不处理")
                                }
                            }
                        } catch (e: Exception) {
                            activityVar.scaffoldState?.showSnackbar(message = "解析失败")
                        }
                    }
                }
            }
        }
    }

    /** 发送文本到会话 */
    private fun postText(action: Action.PostSessionText) {
        val data = action.text

        val suc = repository.webSocketSend(RawTextData(TransferBiz.buildPostDTO(data).toJsonStr()))
        viewModelScope.launch {
            vmCollection?.scaffoldState?.showSnackbar(
                message = if (suc) {
                    "成功发送"
                } else {
                    "发送失败"
                }
            )
        }
    }

    /** 接受会话中的文件 */
    private fun handleByteData(binary: RawDataBase.RawByteData) {
        // 把binary转成 MessageBinaryDTO
        val messageBinaryDTO = MessageBinaryDTO.parseFrom(binary.bytes)
        val contentId = messageBinaryDTO.contentId
        SwithunLog.d("handleByteData contentId: $contentId")

        when (val seq = messageBinaryDTO.seq) {
            0 -> { // seq为0，payload为文件名
                try {
                    SwithunLog.d("handle 0")
                    val appExternalPath = Config.pathConfig.appExternalPath
                    val postFileClientDownloadPath = Config.pathConfig.postFileClientDownloadPath
                    // 获取文件名
                    val fileName = messageBinaryDTO.payload.utf8()
                    val file = File(
                        "$appExternalPath$postFileClientDownloadPath",
                        fileName
                    )
                    SwithunLog.d("handleByteData file: ${file.absolutePath}")

                    // 递归创建父文件夹(如果不存在的话)
                    file.parentFile?.mkdirs()

                    // 检查文件是否存在，存在则删除
                    if (file.exists()) {
                        file.delete().nullCheck("delete old file", true)
                    }
                    // 创建新文件
                    file.createNewFile().nullCheck("create new file", true)
                    receivingFileMap[contentId] = fileName
                } catch (e: java.lang.Exception) {
                    SwithunLog.e("handleByteData error: ${e.message}")
                }
            }
            -1 -> { // seq为-1，表示文件接收完成
                viewModelScope.launch {
                    SwithunLog.d("handle -1")
                    vmCollection?.scaffoldState?.showSnackbar(message = "文件接收完成")
                }
            }
            else -> { // seq为其他值，payload为文件内容
                try {
                    SwithunLog.d("handle $seq")
                    val appExternalPath = Config.pathConfig.appExternalPath
                    val postFileClientDownloadPath = Config.pathConfig.postFileClientDownloadPath

                    val fileName = receivingFileMap[contentId] ?: return
                    val file = File("$appExternalPath$postFileClientDownloadPath", fileName)
                    // 根据seq计算写入位置（seq * 60kB）
                    val offset = (messageBinaryDTO.seq - 1) * 60 * 1024

                    val raf = RandomAccessFile(file, "rw")
                    raf.seek(offset.toLong())
                    raf.write(messageBinaryDTO.payload.toByteArray())
                    raf.close()
                } catch (e: java.lang.Exception) {
                    SwithunLog.e("handleByteData error 1: ${e.message}")
                }
            }
        }
    }

    /** 发送文件到会话 */
    private fun postSessionFile(action: Action.PostSessionFile) {
        val uri = action.uri
        val context = action.context

        viewModelScope.launch(Dispatchers.IO) {

            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bufferSize = 60 * 1024
                    val buffer = ByteArray(bufferSize)

                    var seq = 0
                    val contentId = UUID.randomUUID().toString()

                    val fileName: String = context.contentResolver.query(
                        uri, null, null, null, null
                    )?.use { cursor ->
                        cursor.moveToFirst()
                        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).let {
                            if (it >= 0) {
                                cursor.getString(it)
                            } else {
                                "unknown"
                            }
                        }
                    } ?: "unknown"

                    // 文件存储路径
                    val filePath = "${action.parentPath}/$fileName"
                    val filePathBytes = filePath.encodeToByteArray()
                    val filePathDTO =
                        MessageBinaryDTO(contentId, seq, ByteString.of(*filePathBytes))
                    repository.webSocketSuspendSend(RawDataBase.RawByteData(ByteString.of(*filePathDTO.toByteArray())))

                    seq++

                    while (true) {
                        SwithunLog.d("send $seq")
                        val bytesRead = inputStream.read(buffer)
                        if (bytesRead == -1) {
                            break
                        }
                        val payload = buffer.sliceArray(0 until bytesRead)
                        val message = MessageBinaryDTO(contentId, seq, ByteString.of(*payload))
                        val messageBytes = message.toByteArray()
                        repository.webSocketSuspendSend(RawDataBase.RawByteData(ByteString.of(*messageBytes)))
                        seq++
                    }

                    val finalDTO = MessageBinaryDTO(contentId, -1, ByteString.of())
                    repository.webSocketSuspendSend(RawDataBase.RawByteData(ByteString.of(*finalDTO.toByteArray())))

                }
            } catch (e: Exception) {
                SwithunLog.e("err : $e")
            }
        }
    }

    /** 请求下载会话中的文件 */
    private fun getSessionFile(action: ConnectServerViewModel.Action.GetSessionFile) {
        val appExternalPath = Config.pathConfig.appExternalPath
        val postFileServerCachePath = Config.pathConfig.postFileServerCachePath

        val fileName = action.fileName
        val filePath =
            "$appExternalPath$postFileServerCachePath/$fileName"
        repository.webSocketSend(
            RawTextData(TransferBiz.buildGetDTO(filePath).toJsonStr())
        )
    }

    private suspend fun translate(data: RawTextData) {
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
    private fun getDigest(string: String?): String? {
        if (string == null) {
            return null
        }
        val hexDigits = charArrayOf(
            '0',
            '1',
            '2',
            '3',
            '4',
            '5',
            '6',
            '7',
            '8',
            '9',
            'A',
            'B',
            'C',
            'D',
            'E',
            'F'
        )
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

    private fun truncate(q: String?): String? {
        if (q == null) {
            return null
        }
        val len = q.length
        return if (len <= 20) q else q.substring(0, 10) + len + q.substring(len - 10, len)
    }

}

data class WordsResult(
    val word: String,
    val explains: List<String>,
    val translation: String,
)

