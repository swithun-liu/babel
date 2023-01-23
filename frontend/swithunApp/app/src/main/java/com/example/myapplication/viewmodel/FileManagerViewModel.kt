package com.example.myapplication.viewmodel

import android.os.Environment
import androidx.lifecycle.ViewModel

class FileManagerViewModel: ViewModel() {
    val fileBasePath = Environment.getExternalStorageDirectory().absolutePath
}