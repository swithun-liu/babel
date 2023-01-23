package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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

    init {
        initVideoServer()
    }

    private fun initVideoServer() {
        getBasePath(server)
        getFilesPath(server)
        postFilePath(server)
        server.listen(asyncServer, 54321)
        SwithunLog.d("start http server")
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