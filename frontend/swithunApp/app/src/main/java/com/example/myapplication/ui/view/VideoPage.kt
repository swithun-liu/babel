package com.example.myapplication.ui.view

import android.graphics.SurfaceTexture
import android.os.Build
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
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
//    AndroidView(
//        factory = { context ->
//            SwithunLog.d("AndroidView # factory")
//            val surfaceView = SurfaceView(context)
//            surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
//                override fun surfaceCreated(holder: SurfaceHolder) {
//                    // surfaceView在activity Stop时会destroy，重新切到前台会重新走create，这里要重新setDisplay
//                    // 否则会黑屏但是有声音 https://github.com/Bilibili/ijkplayer/issues/2666#issuecomment-800083756
//                    player.setDisplay(holder)
//                    player.start()
//                }
//
//                override fun surfaceChanged(
//                    holder: SurfaceHolder,
//                    format: Int,
//                    width: Int,
//                    height: Int,
//                ) {
//                }
//
//                override fun surfaceDestroyed(holder: SurfaceHolder) {
//                    player.pause()
//                }
//
//            })
//
//            activityVar.mySurfaceView = surfaceView
//
//            surfaceView
//        },
//        modifier = Modifier.width(100.dp).height(100.dp),
//        update = {
//            SwithunLog.d("AndroidView # update")
//        })

    AndroidView(modifier = Modifier
        .width(100.dp)
        .height(100.dp), factory = { context ->
        val textureView = TextureView(context)
        textureView.post {
            SwithunLog.d("swithun-xxxx post - ${textureView.layoutParams}")
            textureView.layoutParams.width = 100
            textureView.layoutParams.height = 100
            SwithunLog.d("swithun-xxxx post - ${textureView.layoutParams}")
        }
        textureView.surfaceTextureListener = object : SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                SwithunLog.d("surface av")
                //player.setSurface(Surface(p0))
                activityVar.videoVM.player.setSurface(Surface(p0))
                activityVar.textureView = p0
                //textureView.setSurfaceTexture(p0)
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
                SwithunLog.d("surface up")
                activityVar.textureView = p0
            }

        }
        //activityVar.textureView = textureView

        textureView
    })
}