package com.example.myapplication.ui.view

import android.graphics.SurfaceTexture
import android.os.Build
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
import com.example.myapplication.SwithunLog
import com.example.myapplication.model.ActivityVar
import tv.danmaku.ijk.media.player.IjkMediaPlayer

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun VideoPage(activityVar: ActivityVar) {
    IjkPlayer(player = activityVar.videoVM.player, activityVar)
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun IjkPlayer(player: IjkMediaPlayer, activityVar: ActivityVar) {
    // https://juejin.cn/post/7034363130121551903
    AndroidView(
        modifier = Modifier
            .defaultMinSize(100.dp)
            .fillMaxHeight()
            .aspectRatio(ratio = activityVar.videoVM.uiState.aspectRatio)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        factory = { context ->
            val textureView = TextureView(context)
            textureView.surfaceTextureListener = object : SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                    SwithunLog.d("surface av")
                    activityVar.videoVM.player.setSurface(Surface(p0))
                    activityVar.textureView = p0
                }

                override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
                    SwithunLog.d("surface sc")
                    activityVar.textureView = p0
                }

                override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                    SwithunLog.d("surface des")
                    player.pause()
                    return true
                }

                override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
                }

            }

            textureView
        })
}