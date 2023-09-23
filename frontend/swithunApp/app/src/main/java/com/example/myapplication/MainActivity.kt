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
import com.koushikdutta.async.future.Continuation
import com.swithun.usb_mass_storage_exfat.UsbMassStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.jahnen.libaums.core.UsbMassStorageDevice
import java.io.File
import kotlin.coroutines.resume


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
    private val continuationStore = mutableMapOf<Int, kotlin.coroutines.Continuation<*>>()

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

        printExternalPaht()

        lifecycleScope.launch(Dispatchers.IO) {
            SwithunLog.d("usb begin readWriteExternalResult")
            val readWriteExternalResult = AuthChecker.checkWriteExternalStorageV2(this@MainActivity) { code, continuation ->
                continuationStore.put(code, continuation)
            }
            SwithunLog.d("usb readWriteExternalResult")
            val managerExternal = AuthChecker.checkManagerExternalStorage(this@MainActivity) { code, continuation ->
                continuationStore.put(code, continuation)
            }

            val path = StorageUtils.getUsbDir(this@MainActivity).nullCheck("usb new path", true)
            val externalUsb = AuthChecker.checkUsb(this@MainActivity, path ?: "") { code, continuation ->
                continuationStore.put(code, continuation)
            }
            SwithunLog.d("usb readWriteExternalResult")

            testFile()
            document()
            testLegle()
        }

        lifecycleScope.launch {
            delay(10000)
            document()
        }

//        liamus()
//        testMyUsb()


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            testMediaUsb()
        }

//        getSDCardPath()

//        test()

//        requestmanageexternalstorage_Permission()
    }

    private fun printExternalPaht() {
        val path = Environment.getExternalStorageDirectory().absolutePath
        // 打印path
        Log.d("swithun-xxxx", "usb inner out path: $path")
    }

    private fun testFile() {
        val file = File("/storage/emulated/0/documents/")
        val children = file.listFiles().nullCheck("usb file.listFiles()", true)
        // 打印
        children?.forEach {
            Log.d("swithun-xxxx", "usb file.listFiles(): ${it.name}")
        }
    }

    private fun document() {

        val path = StorageUtils.getUsbDir(this).nullCheck("usb new path", true)


        path?.let { path ->

            lifecycleScope.launch {
//                showOpenDocumentTree(path)
//                delay(10000)

                val files = File(path).listFiles().nullCheck("usb new list files", true)
                files?.forEach {
                    SwithunLog.d("usb file: ${it.name}")
                }
            }

        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        SwithunLog.d("usb onActivityResult: $requestCode resultCode: $resultCode data: $data");
        when (requestCode) {
            AuthChecker.ExternalManager -> {
                (continuationStore.remove(requestCode) as? kotlin.coroutines.Continuation<Boolean>)?.resume(true)
            }
            AuthChecker.ExternalUsb -> {
                (continuationStore.remove(requestCode) as? kotlin.coroutines.Continuation<Boolean>)?.resume(true)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        SwithunLog.d("usb onRequestPermissionsResult: $requestCode")
        when (requestCode) {
            AuthChecker.REQUEST_CODE_CONTACT -> {
                (continuationStore.remove(requestCode) as? kotlin.coroutines.Continuation<Boolean>)?.resume(true)
            }
        }
    }

    private fun liamus() {
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
            Log.d("swithun-xxxx", "usb isLegacy: $isLegacy")
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