package com.example.myapplication.model

import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.ConnectServerViewModel
import com.example.myapplication.MainActivity
import com.example.myapplication.viewmodel.*

class VMDependency constructor(
    var activity: MainActivity,
    val connectKernelVM: ConnectKernelViewModel,
    val connectServerVM: ConnectServerViewModel,
    val videoVM: VideoViewModel,
    val nasVM: NasViewModel,
    val fileVM: FileManagerViewModel,
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