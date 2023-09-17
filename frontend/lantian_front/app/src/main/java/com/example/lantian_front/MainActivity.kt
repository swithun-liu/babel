package com.example.lantian_front

import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.lantian_front.model.KernelConfig
import com.example.lantian_front.model.PathConfig
import com.example.lantian_front.model.ServerConfig
import com.example.lantian_front.model.VMCollection
import com.example.lantian_front.ui.theme.MyApplicationTheme
import com.example.lantian_front.ui.view.Myapp
import com.example.lantian_front.util.AuthChecker
import com.example.lantian_front.util.SPUtil
import com.example.lantian_front.viewmodel.*
import com.example.lantian_front.viewmodel.connectserver.ConnectServerViewModel
import com.example.lantian_front.viewmodel.filemanager.FileManagerViewModel
import com.example.lantian_front.viewmodel.serversetting.Action
import com.example.lantian_front.viewmodel.serversetting.Event
import com.example.lantian_front.viewmodel.serversetting.ServerSettingViewModel
import kotlinx.coroutines.launch


/**
 * [api](https://github.com/SocialSisterYi/bilibili-API-collect/tree/master/login/login_action)
 */

class MainActivity : ComponentActivity() {

    private val connectKernelViewModel: ConnectKernelViewModel by viewModels()
    private val connectServerViewModel: ConnectServerViewModel by viewModels()
    private val videoViewModel: VideoViewModel by viewModels()
    private val nasViewModel: NasViewModel by viewModels()
    private val fileViewModel: FileManagerViewModel by viewModels()
    private val serverSettingViewModel: ServerSettingViewModel by viewModels()

    private val shareViewModel: BusViewModel by viewModels()

    private val vmCollection by lazy {
        VMCollection(
            connectKernelViewModel,
            connectServerViewModel,
            videoViewModel,
            nasViewModel,
            fileViewModel,
            shareViewModel
        )
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        initWindow()
        initConfig()
        initServerSetting()
        initVideo()
        listenBusViewMode()
        super.onCreate(savedInstanceState)
        initView()
        initAuth()
        test()
    }

    private fun test() {
        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        SwithunLog.d("path: $path", "MainActivity", "test")
        // 打印path所有文件
        path.listFiles()?.forEach {
            SwithunLog.d("file: $it", "MainActivity", "test")
        }
    }

    private fun initAuth() {
        AuthChecker.checkWriteExternalStorage(this)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun initView() {
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Myapp()
                }
            }
        }
    }

    private fun initWindow() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private fun initConfig() {
        Config.pathConfig.init(this)
        Config.kernelConfig.init(this)
    }

    private fun initServerSetting() {
        val lastTimeConnectServerIp = SPUtil.ServerSetting.getLastTimeConnectServer(this)
        serverSettingViewModel.reduce(Action.Init(shareViewModel, lastTimeConnectServerIp))
    }

    private fun initVideo() {
        vmCollection.videoVM.initDependency(VideoViewModel.Dependency(
            SPUtil.getString(this, "SESSDATA").nullCheck("get cookieSessionData", true) ?: ""
        ))
        vmCollection.videoVM.init()
    }

    private fun listenBusViewMode() {
        lifecycleScope.launch {
            vmCollection.busViewModel.event.collect {
                when (val event = it as? BusViewModel.Event) {
                    is BusViewModel.Event.NeedActivity -> { }
                    is BusViewModel.Event.ToastEvent -> showToast(event)
                    null -> { }
                }
                when (val event = it as? Event) {
                    is Event.UpdateLastTimeConnectServerIp ->  updateLastTimeConnectServerIp(event)
                    null -> { }
                }
            }
        }
    }

    private suspend fun showToast(event: BusViewModel.Event.ToastEvent) {
        vmCollection.busViewModel.snackbarHostState.showSnackbar(event.text.toString())
        event.block()
    }
    private fun updateLastTimeConnectServerIp(event: Event.UpdateLastTimeConnectServerIp) {
        SPUtil.ServerSetting.putLastTimeConnectServer(this, event.ip)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}

object Config {
    val kernelConfig: KernelConfig = KernelConfig
    val pathConfig: PathConfig = PathConfig
    val serverConfig: ServerConfig = ServerConfig
}