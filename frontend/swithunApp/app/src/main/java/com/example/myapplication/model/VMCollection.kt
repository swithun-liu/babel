package com.example.myapplication.model

import com.example.myapplication.viewmodel.connectserver.ConnectServerViewModel
import com.example.myapplication.viewmodel.*
import com.example.myapplication.viewmodel.filemanager.FileManagerViewModel

class VMCollection constructor(
    val connectKernelVM: ConnectKernelViewModel,
    val connectServerVM: ConnectServerViewModel,
    val videoVM: VideoViewModel,
    val nasVM: NasViewModel,
    val fileVM: FileManagerViewModel,
    var shareViewModel: ShareViewModel,
) {

    init {
        fileVM.init(this)
        connectKernelVM.init(this)
        nasVM.init(this)
        connectServerVM.init(this)
        videoVM.init(this)
    }

}