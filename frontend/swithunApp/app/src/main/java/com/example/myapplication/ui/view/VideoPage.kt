package com.example.myapplication.ui.view

import android.graphics.SurfaceTexture
import android.os.Build
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.SwithunLog
import com.example.myapplication.viewmodel.VideoViewModel
import java.lang.Exception

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun VideoPage() {
    Row {
        IjkPlayer()
        VideoListPage()
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun IjkPlayer(
    videoViewModel: VideoViewModel = viewModel(),
) {
    // https://juejin.cn/post/7034363130121551903
    AndroidView(
        modifier = Modifier
            .defaultMinSize(100.dp)
            .fillMaxHeight()
            .aspectRatio(ratio = videoViewModel.uiState.aspectRatio)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        factory = { context ->
            SwithunLog.d("重建TextureView")
            val textureView = TextureView(context)
            textureView.surfaceTextureListener = object : SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                    SwithunLog.d("surface av")
                    videoViewModel.uiState.player.start()
                    videoViewModel.uiState.player.setSurface(Surface(p0))
                }

                override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
                    SwithunLog.d("surface sc")
                }

                override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                    SwithunLog.d("surface des")
                    videoViewModel.uiState.player.pause()
                    return true
                }

                override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
                }

            }

            textureView
        },
        update = {
            try {
                videoViewModel.uiState.player.setSurface(Surface(it?.surfaceTexture))
            } catch (e: Exception) {

            }
        }
    )
}