package com.example.myapplication

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
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
import com.example.myapplication.util.DocumentsUtils
import com.example.myapplication.util.SPUtil
import com.example.myapplication.util.StorageUtils
import com.example.myapplication.viewmodel.*
import com.example.myapplication.viewmodel.connectserver.ConnectServerViewModel
import com.example.myapplication.viewmodel.filemanager.FileManagerViewModel
import com.google.zxing.integration.android.IntentIntegrator.REQUEST_CODE
import com.swithun.usb_mass_storage_exfat.UsbMassStorage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.jahnen.libaums.core.UsbMassStorageDevice
import java.io.File


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
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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

            val permissionIntent = PendingIntent.getBroadcast(
                this, 0, Intent(
                    ACTION_USB_PERMISSION
                ), PendingIntent.FLAG_IMMUTABLE
            )
            usbManager.requestPermission(device.usbDevice, permissionIntent)
            usbManager.hasPermission(device.usbDevice).nullCheck("usb haspermission", true)

            try {
                SwithunLog.d("usb 1")
                val init = device.init()
                SwithunLog.d("usb 2 : init : $init")
                SwithunLog.d("usb device.partitions: ${device?.partitions?.size}")
                val currentFs = device.partitions?.getOrNull(0)?.fileSystem
                if (currentFs == null) {
                    SwithunLog.d("usb currentFs null")
                    break
                }

                vmCollection.fileVM.initUsbDevices(currentFs)
                vmCollection.nasVM.initUstDevices(currentFs)


                break

            } catch (e: java.lang.Exception) {
                SwithunLog.d("usb exception: $e")
            }
        }

        val path = StorageUtils.getUsbDir(this).nullCheck("usb new path", true)


        path?.let { path ->

            lifecycleScope.launch {
                showOpenDocumentTree(path)
                delay(10000)

                val files = File(path).listFiles().nullCheck("usb new list files", true)
                files?.forEach {
                    SwithunLog.d("usb file: ${it.name}")
                }
            }

        }

        testMyUsb()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            testMediaUsb()
        }

        getSDCardPath()

        testLegle()
        test()

        requestmanageexternalstorage_Permission()
    }

    private fun requestmanageexternalstorage_Permission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 先判断有没有权限
            if (Environment.isExternalStorageManager()) {
                Toast.makeText(
                    this,
                    "Android VERSION  R OR ABOVE，HAVE MANAGE_EXTERNAL_STORAGE GRANTED!",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "Android VERSION  R OR ABOVE，NO MANAGE_EXTERNAL_STORAGE GRANTED!",
                    Toast.LENGTH_LONG
                ).show()
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:" + this.packageName)
                startActivityForResult(intent, REQUEST_CODE)
            }
        }
    }

    private fun test() {
        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        SwithunLog.d("path: $path", "MainActivity", "test")
        // 打印path所有文件
        path.listFiles()?.forEach {
            SwithunLog.d("file: $it", "MainActivity", "test")
        }
    }

    /**
     * 检查是否没有使用 分区存储
     */
    private fun testLegle() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val isLegacy = Environment.isExternalStorageLegacy()
            // 打印日志
            Log.d("swithun-xxxx", "isLegacy: $isLegacy")
        }
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun testMediaUsb() {
        val volumeNames = MediaStore.getExternalVolumeNames(this)
        // Log volumeNames
        for (volumeName in volumeNames) {
            Log.d("swithun-xxxx", "volumeName: $volumeName")
        }


        val projection = arrayOf<String>(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.TITLE)

        val volumeAudioUri = Media.getContentUri("0123-4567");
        this.contentResolver.query(volumeAudioUri, projection, null, null, null).use { cursor ->
            // Cache column indices.
            val idColumn = cursor?.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val artistColumn = cursor?.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val titleColumn = cursor?.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)

            while (cursor?.moveToNext() == true) {
                // Get values of columns for a given video.
                val id = cursor.getLong(idColumn!!)
                val artist = cursor.getString(artistColumn!!)
                val title = cursor.getString(titleColumn!!)

                // Do something with the values.
                Log.d("swithun-xxxx", "id: $id")
                Log.d("swithun-xxxx", "artist: $artist")
                Log.d("swithun-xxxx", "title: $title")
                // Use the videos uri to refer to the video.
                // e.g. Uri contentUri = ContentUris.withAppendedId(
                //        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
            }
        }

//        val a = Environment.getExternalStorageDirectory()
//        // 打印a
//        Log.d("swithun-xxxx", "a: $a")
//        // 打印a.listFiles()
//        a.listFiles()?.forEach {
//            Log.d("swithun-xxxx", "a.listFiles(): ${it.name}")
//        }
    }

    private fun testMyUsb() {
        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val usbDevices = usbManager.deviceList

        SwithunLog.d("usb # usbDevices size: ${usbDevices.size}}")

        lifecycleScope.launch {

            for ((key, device) in usbDevices) {
                SwithunLog.d("usb # requestPermission $key ${device.deviceId}")
                val intent = PendingIntent.getBroadcast(
                    this@MainActivity, 0, Intent(
                        ACTION_USB_PERMISSION
                    ), PendingIntent.FLAG_IMMUTABLE
                )
                if (usbManager.hasPermission(device)) {
                    SwithunLog.d("usb # requestPermission has permission $key ${device.deviceId}")
                } else {
                    SwithunLog.d("usb # requestPermission request permission $key ${device.deviceId}")
                    usbManager.requestPermission(device, intent)
                }
            }

            delay(10000)

            UsbMassStorage.filterUsbMassStorageFromAllUsbDevices(this@MainActivity)

        }

        val file = File("mnt/media_rw/64EA-D541/")
        // 打印所有子目录名字
        file.listFiles()?.forEach {
            Log.d("swithun-xxxx", "media_rw  file.listFiles(): ${it.name}")
        }
    }

    private fun showOpenDocumentTree(rootPath: String) {
        var intent: Intent? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val sm = getSystemService(StorageManager::class.java)
            val volume = sm.getStorageVolume(File(rootPath))
            if (volume != null) {
                intent = volume.createAccessIntent(null)
            }
        }
        if (intent == null) {
            intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        }
        Log.d("MainActivity", "startActivityForResult...")
        startActivityForResult(intent, DocumentsUtils.OPEN_DOCUMENT_TREE_CODE)
    }

    public fun getSDCardPath() {
        val sdk_paht  = Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)
        // 打印log
        Log.d("swithun-xxxx", "sdk_path: $sdk_paht")
        val sdDir = Environment.getExternalStorageDirectory()
        // 打印sdkDir
        Log.d("swithun-xxxx", "sdDir: $sdDir")
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