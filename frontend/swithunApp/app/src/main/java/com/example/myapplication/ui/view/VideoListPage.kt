package com.example.myapplication.ui.view

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.SwithunLog
import com.example.myapplication.framework.Async
import com.example.myapplication.model.ActivityVar
import com.example.myapplication.model.SectionItem
import com.example.myapplication.util.HeaderParams
import com.example.myapplication.viewmodel.VideoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun VideoListPage(activityVar: ActivityVar) {
    Row {
        ConanVideoView()
        QRCode(activityVar.videoVM)
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun ConanVideoView(
    videoViewModel: VideoViewModel = viewModel(),
) {
    val onGetConanUrl = handler@{ conanUrl: String ->
        val headerParams = HeaderParams().apply { setBilibiliReferer() }
        try {
            // 循环播放
            videoViewModel.reduce(VideoViewModel.Action.PlayVideoAction(
                conanUrl, headerParams, { }
            ))
            // 播放进度计算
            videoViewModel.viewModelScope.launch {
                while (true) {
                    delay(500)
                    val duration = when (val duration = videoViewModel.player.duration) {
                        0L -> 1F
                        else -> duration.toFloat()
                    }
                    videoViewModel.reduce(VideoViewModel.Action.UpdateCurrentVideoProcess(
                        videoViewModel.player.currentPosition.toFloat() / duration
                    ))
                }
            }
        } catch (e: Error) {
            SwithunLog.e("player err")
        }
    }

    Row {
        Column {
            Button(onClick = {
                if (videoViewModel.player.isPlaying) {
                    videoViewModel.player.pause()
                } else {
                    videoViewModel.player.start()
                }
            }) {
                Text(text = "stop")
            }
            Text(text = videoViewModel.uiState.currentProcess.toString())
        }

        LazyColumn(
            modifier = Modifier
                .background(Color(R.color.purple_200))
                .width(Dp(100f))
        ) {
            items(videoViewModel.uiState.itemList) { sectionItem: SectionItem ->
                Button(onClick = {
                    videoViewModel.viewModelScope.launch(Dispatchers.IO) {
                        videoViewModel.getConanByEpId(sectionItem.id)?.let { newConanUrl ->
                            onGetConanUrl(newConanUrl)
                        }
                    }
                }) {
                    Text(text = "${sectionItem.shortTitle}: ${sectionItem.longTitle}")
                }
            }
        }
    }
}

@Composable
fun QRCode(
    videoVM: VideoViewModel,
) {
    Column(verticalArrangement = Arrangement.Top) {
        Text(text = videoVM.uiState.loginStatus)
        Image(painter = BitmapPainter(videoVM.uiState.qrCodeImage), contentDescription = "qrCode")
    }
}