package com.example.myapplication.model

import android.view.SurfaceView
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ScaffoldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.ConnectServerViewModel
import com.example.myapplication.MainActivity
import com.example.myapplication.viewmodel.*

class ActivityVar @OptIn(ExperimentalMaterial3Api::class) constructor(
    var activity: MainActivity,
    val kernelConfig: KernelConfig = KernelConfig( { activity }),
    val serverConfig: ServerConfig = ServerConfig,
    var mySurfaceView: SurfaceView? = null,
    val connectVM: ConnectKernelViewModel = ViewModelProvider(
        activity,
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return ConnectKernelViewModel() as T
            }
        }).get(ConnectKernelViewModel::class.java),
    val connectServerVM: ConnectServerViewModel = ViewModelProvider(
        activity,
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return ConnectServerViewModel() as T
            }

        }).get(ConnectServerViewModel::class.java),
    val videoVM: VideoViewModel = ViewModelProvider(activity, object : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return VideoViewModel { activity } as T
        }
    }).get(VideoViewModel::class.java),
    val ftpVM: FTPViewModel = ViewModelProvider(activity, object : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return FTPViewModel { activity } as T
        }
    }).get(FTPViewModel::class.java),
    val nasVM: NasViewModel = ViewModelProvider(activity, object : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return NasViewModel { activity } as T
        }
    }).get(NasViewModel::class.java),
    val fileManagerViewModel: FileManagerViewModel = ViewModelProvider(activity).get(
        FileManagerViewModel::class.java
    ),
    var scaffoldState: ScaffoldState? = null
) {

    init {
        fileManagerViewModel.init(this)
        connectVM.init(this)
        nasVM.init(this)
        connectServerVM.init(this)
    }

}