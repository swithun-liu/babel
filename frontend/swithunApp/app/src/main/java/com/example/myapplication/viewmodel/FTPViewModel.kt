package com.example.myapplication.viewmodel

import android.app.Activity
import android.os.Environment
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.SwithunLog
import kotlinx.coroutines.*
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.ftplet.*
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.usermanager.PasswordEncryptor
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.apache.ftpserver.usermanager.impl.WritePermission
import java.io.*
import java.lang.Exception


class FTPViewModel(val activity: () -> Activity) : ViewModel() {

    private var client2221: FTPClient? = null
    private var client5656: FTPClient? = null

    fun initFTP() {

        viewModelScope.launch(Dispatchers.IO) {

            val fileBasePath = Environment.getExternalStorageDirectory().absolutePath
            Log.i("MainActivity", fileBasePath)
            var propertisPath = "file:///android_asset/users.properties"

            val root = File(fileBasePath)
            SwithunLog.d("fileBasePath listFiles: ${root.listFiles()}")


            val serverFactory = FtpServerFactory()
            val factory = ListenerFactory()
            factory.port = 2221// set the port of the listener (choose your desired port, not 1234)
            serverFactory.addListener("default", factory.createListener())
            val userManagerFactory = PropertiesUserManagerFactory()
            //userManagerFactory.file =
            // File(propertisPath)//choose any. We're telling the FTP-server where to read it's user list
            userManagerFactory.passwordEncryptor =
                object : PasswordEncryptor {//We store clear-text passwords in this example

                    override fun encrypt(password: String): String {
                        return password
                    }

                    override fun matches(passwordToCheck: String, storedPassword: String): Boolean {
                        return passwordToCheck == storedPassword
                    }
                }
            //Let's add a user, since our myusers.properties files is empty on our first test run
            val user = BaseUser()
            user.name = "test"
            user.password = "test"
            user.homeDirectory = fileBasePath
            val authorities = ArrayList<Authority>()
            authorities.add(WritePermission())
            user.authorities = authorities
            val um = userManagerFactory.createUserManager()
            try {
                um.save(user)//Save the user to the user list on the filesystem
            } catch (e1: FtpException) {
                //Deal with exception as you need
            }

            serverFactory.userManager = um
            val m = HashMap<String, Ftplet>()
            m["miaFtplet"] = object : Ftplet {

                @Throws(FtpException::class)
                override fun init(ftpletContext: FtpletContext) {
                    System.out.println("init");
                    System.out.println("Thread #" + Thread.currentThread().getId());
                }

                override fun destroy() {
                    System.out.println("destroy");
                    System.out.println("Thread #" + Thread.currentThread().getId());
                }

                @Throws(FtpException::class, IOException::class)
                override fun beforeCommand(session: FtpSession, request: FtpRequest): FtpletResult {
                    System.out.println("beforeCommand " + session.getUserArgument() + " : " + session.toString() + " | " + request.getArgument() + " : " + request.getCommand() + " : " + request.getRequestLine());
                    System.out.println("Thread #" + Thread.currentThread().getId());

                    //do something
                    return FtpletResult.DEFAULT//...or return accordingly
                }

                @Throws(FtpException::class, IOException::class)
                override fun afterCommand(
                    session: FtpSession,
                    request: FtpRequest,
                    reply: FtpReply
                ): FtpletResult {
                    System.out.println("afterCommand " + session.getUserArgument() + " : " + session.toString() + " | " + request.getArgument() + " : " + request.getCommand() + " : " + request.getRequestLine() + " | " + reply.getMessage() + " : " + reply.toString());
                    System.out.println("Thread #" + Thread.currentThread().getId());

                    //do something
                    return FtpletResult.DEFAULT//...or return accordingly
                }

                @Throws(FtpException::class, IOException::class)
                override fun onConnect(session: FtpSession): FtpletResult {
                    System.out.println("onConnect " + session.getUserArgument() + " : " + session.toString());System.out.println(
                        "Thread #" + Thread.currentThread().getId()
                    );

                    //do something
                    return FtpletResult.DEFAULT//...or return accordingly
                }

                @Throws(FtpException::class, IOException::class)
                override fun onDisconnect(session: FtpSession): FtpletResult {
                    System.out.println("onDisconnect " + session.getUserArgument() + " : " + session.toString());
                    System.out.println("Thread #" + Thread.currentThread().getId());

                    //do something
                    return FtpletResult.DEFAULT//...or return accordingly
                }
            }
            serverFactory.ftplets = m
            //Map<String, Ftplet> mappa = serverFactory.getFtplets();
            //System.out.println(mappa.size());
            //System.out.println("Thread #" + Thread.currentThread().getId());
            //System.out.println(mappa.toString());
            val server = serverFactory.createServer()
            try {
                server.start()//Your FTP server starts listening for incoming FTP-connections, using the configuration options previously set
            } catch (ex: FtpException) {
                //Deal with exception as you need
            }
        }

    }

    fun connectFTP(port: Int, job: ((FTPClient) -> Any)? = null) =
        viewModelScope.launch (Dispatchers.IO) {
            try {
                val ftpClient = FTPClient().apply {
                    defaultTimeout = 10000
                    connectTimeout = 10000
                    setDataTimeout(10000)
                }

                if (port == 2221) {
                    client2221 = ftpClient
                } else if (port == 5656) {
                    client5656 = ftpClient
                }

                SwithunLog.d("start connect")
                ftpClient.connect("192.168.0.107", port)
                SwithunLog.d("start login")
                ftpClient.login("test", "test")
                SwithunLog.d("start enterLocalActiveMode")
                ftpClient.enterLocalActiveMode()
                SwithunLog.d("start controlEncoding ")
                ftpClient.controlEncoding = "utf-8"

                val reply = ftpClient.replyCode
                if (!FTPReply.isPositiveCompletion(reply)) {
                    SwithunLog.e("登陆失败: $reply")
                } else {
                    SwithunLog.d("登陆成功")
                }

                SwithunLog.d(
                    "目录: ${ftpClient.listFiles().map {
                        it.name
                    }}"
                )

                job?.invoke(ftpClient)

            } catch (e: Exception) {
                SwithunLog.e("无法连接")
            }

        }

    fun listFTP(port: Int) {
        when (port) {
            2221 -> {
                try {
                    SwithunLog.d(client2221?.listFiles()?.map { it.name })
                } catch (e: Exception) {
                    SwithunLog.e("list failed")
                }
            }
            5656 -> {
                try {
                    SwithunLog.d(client5656?.listFiles()?.map { it.name })
                } catch (e: Exception) {
                    SwithunLog.e("list failed")
                }
            }
        }
    }


    suspend fun downloadFile(port: Int): String {
        var url = ""
        connectFTP(port) { client ->
            try {
                SwithunLog.e("job")
                SwithunLog.d(client.listFiles()?.map { it.name })
                client.changeWorkingDirectory("swithun")
                SwithunLog.d(client.listFiles()?.map { it.name })
                url = "ftp://test:test@192.168.0.107:$port/swithun/gt_2.mp4"
            } catch (e: Exception) {
                SwithunLog.e("list failed")
                return@connectFTP ""
            }
        }.join()

        return url
    }

}

// https://www.jianshu.com/p/8802cb2a0db5