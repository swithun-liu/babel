package com.example.myapplication.viewmodel.connectserver

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.myapplication.Config
import com.example.myapplication.SwithunLog
import com.example.myapplication.model.*
import com.example.myapplication.util.postRequest
import com.example.myapplication.util.safeGetString
import com.example.myapplication.framework.BaseViewModel
import com.example.myapplication.nullCheck
import com.example.myapplication.viewmodel.NasViewModel
import com.example.myapplication.viewmodel.ShareViewModel
import com.example.myapplication.viewmodel.biz.TransferBiz
import com.example.myapplication.websocket.RawDataBase
import com.example.myapplication.websocket.WebSocketRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.ByteString
import java.io.File
import java.io.RandomAccessFile
import java.math.RoundingMode
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.DecimalFormat
import java.util.UUID
import kotlin.coroutines.resume


class ConnectServerViewModel : BaseViewModel<ConnectServerViewModel.Action, Unit, Unit>() {

    private var remoteWordFlow: Flow<RawDataBase>? = null
    var wordsResult by mutableStateOf(WordsResult("", emptyList(), ""))
    private val repository = WebSocketRepository()
    private val receiver = Receiver()
    private val sender = Sender()

    private val YOUDAO_URL = "https://openapi.youdao.com/api"
    private val APP_KEY = "03f39bd127e854a5"
    private val APP_SECRET = "SQJK5f6cfEjHMdP6blgC8OCFlgsLQDCq"

    private val TAG = "swithun{WordsViewModel"

    var vmCollection: VMCollection? = null

    var receivedData by mutableStateOf(listOf<TransferData>())

    // contentId - 文件名
    private var rememberReceivingFile = mutableMapOf<String, String>()
    // contentId
    private var rememberSendingFile = mutableMapOf<String, () -> Unit>()

    fun init(vmCollection: VMCollection) {
        this.vmCollection = vmCollection
    }

    override fun getInitialUIState(): Unit {
        return Unit
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
            is Action.PostSessionText -> sender.postText(action)
            is Action.PostSessionFile -> sender.postSessionFile(action)
            is Action.GetSessionFile -> sender.getSessionFile(action)
        }
    }

    private fun connectServer(action: Action.ConnectServer) {
        // 检查必须数据
        val vmCollection = this.vmCollection ?: return

        // 更新数据
        val serverIp = action.serverIp
        Config.serverConfig.serverIP = serverIp
        vmCollection.nasVM.reduce(NasViewModel.Action.ChangeLastTimeConnectServer(serverIp))

        // 处理数据
        val url = "http://${ServerConfig.serverHost}/${ServerConfig.ServerPath.WebSocketPath.path}"
        remoteWordFlow = repository.webSocketCreate(url, viewModelScope, "Client")
        viewModelScope.launch(Dispatchers.IO) {
            remoteWordFlow?.collect { receiver.handleReceive(it) }
        }
    }

    inner class Receiver {
        suspend fun handleReceive(data: RawDataBase) {
            when (data) {
                is RawDataBase.RawTextData -> handleReceiveTextData(data)
                is RawDataBase.RawByteData -> handleReceiveByteData(data)
            }
        }

        /** 接收server纯文本 */
        private suspend fun handleReceiveTextData(data: RawDataBase.RawTextData) {
            val vmCollection = vmCollection ?: return

            SwithunLog.d("remoteWordFlow collect RawTextData ${data.json}")

            try {
                val message: MessageTextDTO = Gson().fromJson(data.json, MessageTextDTO::class.java)
                when (MessageTextDTO.OptionCode.fromValue(message.code)) {
                    MessageTextDTO.OptionCode.MESSAGE_TO_SESSION -> handleReceivePostSessionText(message)
                    MessageTextDTO.OptionCode.CLIENT_FILE_TO_SESSION_PIECE_ACKNOWLEDGE -> handleReceiveSendFilePieceResponse(message)
                    null -> vmCollection.shareViewModel.snackbarHostState.showSnackbar(message = "无code")
                    else -> vmCollection.shareViewModel.snackbarHostState.showSnackbar(message = "未知code")
                }
            } catch (e: Exception) {
                vmCollection.shareViewModel.snackbarHostState.showSnackbar(message = "解析失败 非MessageTextDTO格式")
            }
        }

        /** 接收server文件 */
        private fun handleReceiveByteData(binary: RawDataBase.RawByteData) {
            SwithunLog.d("remoteWordFlow collect RawByteData")

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
                        rememberReceivingFile[contentId] = fileName
                    } catch (e: java.lang.Exception) {
                        SwithunLog.e("handleByteData error: ${e.message}")
                    }
                }
                -1 -> { // seq为-1，表示文件接收完成
                    viewModelScope.launch {
                        SwithunLog.d("handle -1")
                        vmCollection?.shareViewModel?.snackbarHostState?.showSnackbar(message = "文件接收完成")
                    }
                }
                else -> { // seq为其他值，payload为文件内容
                    try {
                        SwithunLog.d("handle $seq")
                        val appExternalPath = Config.pathConfig.appExternalPath
                        val postFileClientDownloadPath = Config.pathConfig.postFileClientDownloadPath

                        val fileName = rememberReceivingFile[contentId] ?: return
                        val file = File("$appExternalPath$postFileClientDownloadPath", fileName)
                        // 根据seq计算写入位置（seq * 60kB）
                        val offset = (messageBinaryDTO.seq - 1) * Config.serverConfig.fileChunkSize * 1024

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

        private fun handleReceiveSendFilePieceResponse(message: MessageTextDTO) {
            val content_id = message.content
            rememberSendingFile.remove(content_id)?.invoke()
        }

        private suspend fun handleReceivePostSessionText(message: MessageTextDTO) {
            val vmCollection = vmCollection ?: return

            val oodList = receivedData
            SwithunLog.d("oodList: $oodList")
            when (MessageTextDTO.ContentType.fromValue(message.content_type)) {
                null -> {
                    vmCollection.shareViewModel.snackbarHostState.showSnackbar(message = "未知 内容类型")
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
    }

    inner class Sender {

        /** 发送文本到会话 */
        fun postText(action: Action.PostSessionText) {
            val data = action.text

            val suc = repository.webSocketSend(RawDataBase.RawTextData(TransferBiz.buildPostDTO(data).toJsonStr()))
            viewModelScope.launch {
                vmCollection?.shareViewModel?.snackbarHostState?.showSnackbar(
                    message = if (suc) {
                        "成功发送"
                    } else {
                        "发送失败"
                    }
                )
            }
        }

        /** 发送文件到会话 */
        fun postSessionFile(action: Action.PostSessionFile) {
            val uri = action.uri
            val context = action.context

            viewModelScope.launch(Dispatchers.IO) {

                val beginTime = System.currentTimeMillis()

                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        // 准备：设置
                        val bufferSize = Config.serverConfig.fileChunkSize * 1024
                        val buffer = ByteArray(bufferSize)

                        // 准备：文件信息
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

                        // 发送：0 first package
                        var seq = 0
                        val contentId = UUID.randomUUID().toString()

                        val filePath = "${action.parentPath}/$fileName" // 文件存储路径
                        val filePathBytes = filePath.encodeToByteArray()
                        val filePathDTO =
                            MessageBinaryDTO(contentId, seq, ByteString.of(*filePathBytes))
                        sendAndContinueAfterResponse(filePathDTO)

                        seq++

                        // 发送：1..(n-1) sequence package
                        while (true) {
                            SwithunLog.d("send $seq")
                            val bytesRead = inputStream.read(buffer)
                            if (bytesRead == -1) {
                                break
                            }
                            val payload = buffer.sliceArray(0 until bytesRead)
                            val message = MessageBinaryDTO(contentId, seq, ByteString.of(*payload))
                            val messageBytes = message.toByteArray()
                            sendAndContinueAfterResponse(message)
                            seq++
                        }

                        // 发送：n final package
                        val finalDTO = MessageBinaryDTO(contentId, -1, ByteString.of())
                        sendAndContinueAfterResponse(finalDTO)

                        // 结束：计算传输速度，保留小数点后2位
                        val endTime = System.currentTimeMillis()
                        val speed: Double = (File(filePath).length() / 1024 / 1024) / ((endTime - beginTime) / 1000.0)
                        val df = DecimalFormat("#.##").apply {
                            roundingMode = RoundingMode.DOWN
                        }
                        SwithunLog.d("speed: ${df.format(speed)} MB/s")
                        vmCollection?.shareViewModel?.reduce(
                            ShareViewModel.Action.ToastAction(
                                "文件传输完成: ${
                                    df.format(
                                        speed
                                    )
                                } MB/s".toTextRes()
                            )
                        )
                    }
                } catch (e: Exception) {
                    SwithunLog.e("err : $e")
                }
            }
        }


        /** 请求下载会话中的文件 */
        fun getSessionFile(action: Action.GetSessionFile) {
            val appExternalPath = Config.pathConfig.appExternalPath
            val postFileServerCachePath = Config.pathConfig.postFileServerCachePath

            val fileName = action.fileName
            val filePath =
                "$appExternalPath$postFileServerCachePath/$fileName"
            repository.webSocketSend(
                RawDataBase.RawTextData(TransferBiz.buildGetDTO(filePath).toJsonStr())
            )
        }


        private suspend fun sendAndContinueAfterResponse(data: MessageBinaryDTO) = suspendCancellableCoroutine<Unit> { continuation ->
            rememberSendingFile[data.contentId] = { continuation.resume(Unit) }
            viewModelScope.launch {
                repository.webSocketSuspendSend(RawDataBase.RawByteData(ByteString.of(*data.toByteArray())))
            }
        }

    }



    private suspend fun translate(data: RawDataBase.RawTextData) {
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

