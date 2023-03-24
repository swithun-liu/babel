package com.example.myapplication.model

import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.SurfaceView
import android.view.TextureView
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
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
    val connectVM: ConnectKernelViewModel = ViewModelProvider(activity,
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
    val fileManagerViewModel: FileManagerViewModel = ViewModelProvider(activity,
        ViewModelProviderFactories(NasViewModel::class.java).getFactory { activity }
    )[FileManagerViewModel::class.java],
    var scaffoldState: SnackbarHostState? = null,
) {

    init {
        fileManagerViewModel.init(this)
        connectVM.init(this)
        nasVM.init(this)
        connectServerVM.init(this)
        videoVM.init(this)
    }

}