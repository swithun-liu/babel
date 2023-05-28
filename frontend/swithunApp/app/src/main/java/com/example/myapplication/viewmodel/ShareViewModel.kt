package com.example.myapplication.viewmodel

import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel

class ShareViewModel: ViewModel() {
    val snackbarHostState: SnackbarHostState = SnackbarHostState()
}