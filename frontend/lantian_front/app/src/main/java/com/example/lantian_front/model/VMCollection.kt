package com.example.lantian_front.model

import com.example.lantian_front.viewmodel.connectserver.ConnectServerViewModel
import com.example.lantian_front.viewmodel.*
import com.example.lantian_front.viewmodel.filemanager.FileManagerViewModel

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