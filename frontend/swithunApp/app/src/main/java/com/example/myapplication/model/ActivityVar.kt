package com.example.myapplication.model

import android.graphics.SurfaceTexture
import android.view.SurfaceView
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.ConnectServerViewModel
import com.example.myapplication.MainActivity
import com.example.myapplication.viewmodel.*

class ActivityVar constructor(
    var activity: MainActivity,
    val kernelConfig: KernelConfig = KernelConfig({ activity }),
    val serverConfig: ServerConfig = ServerConfig,
    var mySurfaceView: SurfaceView? = null,
    var textureView: SurfaceTexture? = null,
    val connectKernelVM: ConnectKernelViewModel = ViewModelProvider(activity,
        ViewModelProviderFactories(ConnectKernelViewModel::class.java).getFactory { activity })[ConnectKernelViewModel::class.java],
    val connectServerVM: ConnectServerViewModel = ViewModelProvider(activity,
        ViewModelProviderFactories(ConnectServerViewModel::class.java).getFactory { activity }
    )[ConnectServerViewModel::class.java],
    val videoVM: VideoViewModel = ViewModelProvider(activity,
        ViewModelProviderFactories(VideoViewModel::class.java).getFactory { activity }
    )[VideoViewModel::class.java],
    val ftpVM: FTPViewModel = ViewModelProvider(activity,
        ViewModelProviderFactories(FTPViewModel::class.java).getFactory { activity }
    )[FTPViewModel::class.java],
    val nasVM: NasViewModel = ViewModelProvider(activity,
        ViewModelProviderFactories(NasViewModel::class.java).getFactory { activity }
    )[NasViewModel::class.java],
    val fileVM: FileManagerViewModel = ViewModelProvider(activity,
        ViewModelProviderFactories(NasViewModel::class.java).getFactory { activity }
    )[FileManagerViewModel::class.java],
    var scaffoldState: SnackbarHostState? = null,
) {

    init {
        fileVM.init(this)
        connectKernelVM.init(this)
        nasVM.init(this)
        connectServerVM.init(this)
        videoVM.init(this)
    }

}