package com.example.myapplication

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.model.KernelConfig
import com.example.myapplication.model.PathConfig
import com.example.myapplication.model.ServerConfig
import com.example.myapplication.model.VMCollection
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.view.Myapp
import com.example.myapplication.util.AuthChecker
import com.example.myapplication.util.SPUtil
import com.example.myapplication.viewmodel.*
import kotlinx.coroutines.launch
import me.jahnen.libaums.core.UsbMassStorageDevice


/**
 * [api](https://github.com/SocialSisterYi/bilibili-API-collect/tree/master/login/login_action)
 */

class MainActivity : ComponentActivity() {

    private val connectKernelViewModel: ConnectKernelViewModel by viewModels()
    private val connectServerViewModel: ConnectServerViewModel by viewModels()
    private val videoViewModel: VideoViewModel by viewModels()
    private val nasViewModel: NasViewModel by viewModels()
    private val fileViewModel: FileManagerViewModel by viewModels()
    private val shareViewModel: ShareViewModel by viewModels()

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
        Config.pathConfig.init(this)
        Config.kernelConfig.init(this)

        lifecycleScope.launch {
            vmCollection.shareViewModel.uiEvent.collect {
                when (it) {
                    is ShareViewModel.Event.NeedActivity -> {
                        it.block(this@MainActivity)
                    }
                    is ShareViewModel.Event.ToastEvent -> {
                        vmCollection.shareViewModel.snackbarHostState.showSnackbar(it.text.toString())
                        it.block()
                    }
                }
            }
        }
        vmCollection.videoVM.initDependency(VideoViewModel.Dependency(
            SPUtil.getString(this, "SESSDATA").nullCheck("get cookieSessionData", true) ?: ""
        ))
        vmCollection.videoVM.init()

        super.onCreate(savedInstanceState)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    // ScreenSetup(activityVar)
                    Myapp()
                }
            }
        }
        AuthChecker.checkWriteExternalStorage(this)

        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val devices = UsbMassStorageDevice.getMassStorageDevices(this)
        Log.d("swithun-xxxx", "devices: ${devices.size}")
        for (device in devices) {

            val permissionIntent = PendingIntent.getBroadcast(this, 0, Intent(
                ACTION_USB_PERMISSION), 0)
            usbManager.requestPermission(device.usbDevice, permissionIntent)
            usbManager.hasPermission(device.usbDevice).nullCheck("usb haspermission", true)

            try {
                SwithunLog.d("usb 1")
                val init = device.init()
                SwithunLog.d("usb 2 : init : $init")
                SwithunLog.d("usb device.partitions: ${device?.partitions?.size}")
                val currentFs = device.partitions?.getOrNull(1)?.fileSystem
                if (currentFs == null) {
                    SwithunLog.d("usb currentFs null")
                    return
                }

                SwithunLog.d("usb Capacity: " + currentFs.capacity)
                SwithunLog.d("usb Occupied Space: " + currentFs.occupiedSpace)
                SwithunLog.d("usb 3")
                SwithunLog.d("usb Free Space: " + currentFs.freeSpace)
                SwithunLog.d("usb Chunk size: " + currentFs.chunkSize)
                SwithunLog.d("usb 4")
                val root = currentFs.rootDirectory
                SwithunLog.d("usb 5")
                val files = root.listFiles()
                for (file in files) {
                    SwithunLog.d("usb file: " + file.name)
                }

            } catch (e: java.lang.Exception) {
                SwithunLog.d("usb exception: $e")
            }
        }

    }

    companion object {
        const val ACTION_USB_PERMISSION = "com.example.myapp.USB_PERMISSION"
    }
}

object Config {
    val kernelConfig: KernelConfig = KernelConfig
    val pathConfig: PathConfig = PathConfig
    val serverConfig: ServerConfig = ServerConfig
}