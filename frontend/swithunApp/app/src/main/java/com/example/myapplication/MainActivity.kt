package com.example.myapplication

import android.os.Build
import android.os.Bundle
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewModelScope
import com.example.myapplication.model.ActivityVar
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.util.AuthChecker
import com.example.myapplication.util.HeaderParams
import com.example.myapplication.viewmodel.*
import com.swithun.liu.ServerSDK
import kotlinx.coroutines.launch


/**
 * [api](https://github.com/SocialSisterYi/bilibili-API-collect/tree/master/login/login_action)
 */

class MainActivity : ComponentActivity() {

    private val activityVar by lazy {
        ActivityVar(this)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        SwithunLog.d("这个 - ${ServerSDK.getTestStr()}")
        SwithunLog.d("这个 - ${ServerSDK.getTestStrWithInput("我是input")}")

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    // ScreenSetup(activityVar)
                    Myapp(activityVar)
                }
            }
        }
        AuthChecker.checkWriteExternalStorage(this)
    }
}


fun playNextConan(
    videoViewModel: VideoViewModel,
    surfaceView: SurfaceView,
    headerParams: HeaderParams
) {
    videoViewModel.viewModelScope.launch {
        val nextConanUrl =
            videoViewModel.getNextConan().nullCheck("get nextConanUrl") ?: return@launch
        videoViewModel.play(nextConanUrl, headerParams) {
            playNextConan(videoViewModel, surfaceView, headerParams)
        }
    }
}


