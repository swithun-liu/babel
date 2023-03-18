package com.example.myapplication.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.model.ActivityVar
import com.example.myapplication.MainActivity
import com.example.myapplication.SwithunLog
import com.example.myapplication.nullCheck
import com.koushikdutta.async.AsyncServer
import com.koushikdutta.async.callback.CompletedCallback
import com.koushikdutta.async.http.body.FilePart
import com.koushikdutta.async.http.body.MultipartFormDataBody
import com.koushikdutta.async.http.server.AsyncHttpServer
import com.koushikdutta.async.http.server.AsyncHttpServerRequest
import com.koushikdutta.async.http.server.AsyncHttpServerResponse
import com.koushikdutta.async.http.server.HttpServerRequestCallback
import com.koushikdutta.async.stream.OutputStreamDataCallback
import com.swithun.liu.ServerSDK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException


class NasViewModel(activity: () -> MainActivity) : ViewModel() {

    // https://juejin.cn/post/6844903551408291848
    // https://github.com/koush/AndroidAsync
    private val server = AsyncHttpServer()
    private val asyncServer = AsyncServer()
    private val fileManagerViewModel: FileManagerViewModel = ViewModelProvider(activity.invoke()).get(FileManagerViewModel::class.java)
    private var activityVar: ActivityVar? = null

    var getAllServerBtnText: String by mutableStateOf("搜寻其他可用server")
    var allServersInLan: List<String> by mutableStateOf(mutableListOf())

    var startMeAsServerBtnText: String by mutableStateOf("启动server：未启动")

    init {
        initVideoServer()
    }


    fun init(activityVar: ActivityVar) {
        this.activityVar = activityVar
    }

    private fun initVideoServer() {
        getBasePath(server)
        getFilesPath(server)
        postFilePath(server)
        server.listen(asyncServer, 54321)
        SwithunLog.d("start http server")
    }

    fun startMeAsServer() {
        viewModelScope.launch(Dispatchers.IO) {
            startMeAsServerBtnText = "启动server：启动中..."
            launch(Dispatchers.IO) {
                delay(500)
                // 连接内核
                startMeAsServerBtnText = "启动server：连接内核中..."
                activityVar?.connectVM?.connectKernel()
                startMeAsServerBtnText = "启动server：已连接内核"
            }
            // 启动内核
            ServerSDK.startSever()
        }
    }

    suspend fun searchAllServer() {
        getAllServerBtnText = "搜寻中..."

        val ips = ServerSDK.getAllServerInLAN()
        allServersInLan = ips.toList()

        getAllServerBtnText = "搜寻其他可用server"
    }

    private fun getBasePath(server: AsyncHttpServer) {
        server.get("/", object : HttpServerRequestCallback {
            override fun onRequest(
                request: AsyncHttpServerRequest?,
                response: AsyncHttpServerResponse?
            ) {
                try {
                    response.nullCheck("/: response", true)
                    response?.send("收到")
                } catch (e: IOException) {
                    SwithunLog.d("server get / response err")
                    response?.code(500)
                }
            }

        })
    }

    private fun getFilesPath(server: AsyncHttpServer) {
        server.get("/files", object : HttpServerRequestCallback {
            override fun onRequest(
                request: AsyncHttpServerRequest?,
                response: AsyncHttpServerResponse?
            ) {
                //val path = "$fileBasePath/swithun/mmm.mp4"
                val path = "${fileManagerViewModel.fileBasePath}/swithun/taxi.mkv"
                val file = File(path).takeIf { it.exists() && it.isFile }.nullCheck("check file exists", true)
                if (file == null) {
                    response?.code(404)?.send("Not found!")
                    return
                }
                try {
                    val fis = FileInputStream(file)

                    // 使用 fis.available() 当文件大于2G时是0
                    // response?.sendStream(fis, fis.available().toLong())
                    response?.sendStream(fis, file.length())
                } catch (e: Exception) {
                    SwithunLog.e("/files : sendStream err")
                }
            }

        })
    }

    private fun postFilePath(server: AsyncHttpServer) {
        var last: FileOutputStream? = null
        server.post("/uploadfile", object : HttpServerRequestCallback {
            override fun onRequest(
                request: AsyncHttpServerRequest?,
                response: AsyncHttpServerResponse?
            ) {
                SwithunLog.d(request)
                val params: MultipartFormDataBody? = request?.getBody<MultipartFormDataBody>()
                params?.setMultipartCallback {
                    it.isFile
                    try {
                        val filePart: FilePart = (it as FilePart)
                        val fos = FileOutputStream("${fileManagerViewModel.fileBasePath}/swithun/${it.filename}))").nullCheck("fos", true)
                        last = fos
                        val osdcb = OutputStreamDataCallback(fos).nullCheck("osdcd", true)
                        params.dataCallback = osdcb
                    } catch (ex: java.lang.Exception) {
                        SwithunLog.d("err")
                    }


                }
                params?.setEndCallback(object : CompletedCallback {
                    override fun onCompleted(ex: java.lang.Exception?) {
                        val a = MultipartFormDataBody()
                        params
                        last
                    }
                })
                response?.send("haha")
            }

        })
    }

}