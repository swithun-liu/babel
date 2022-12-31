package com.example.myapplication

import android.os.Build
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.webkit.WebSettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewModelScope
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.util.HeaderParams
import com.example.myapplication.viewmodel.VideoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tv.danmaku.ijk.media.player.IjkMediaPlayer

/**
 * [api](https://github.com/SocialSisterYi/bilibili-API-collect/tree/master/login/login_action)
 */

class MainActivity : ComponentActivity() {

    private val activityVar = ActivityVar()

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background,
                ) {
                    ScreenSetup(
                        wordsViewModel = WordsViewModel(),
                        videoViewModel = VideoViewModel { this },
                        activityVar
                    )
                }
            }
        }
    }
}

class ActivityVar(
    var mySurfaceView: SurfaceView? = null
)

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun ScreenSetup(
    wordsViewModel: WordsViewModel,
    videoViewModel: VideoViewModel,
    activityVar: ActivityVar,
) {
    Row {
        VideoScreen(
            videoViewModel,
            activityVar
        )
        WordsScreen(
            wordsResult = wordsViewModel.wordsResult,
        ) { wordsViewModel.sendMessage(it) }
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun VideoScreen(
    videoViewModel: VideoViewModel,
    activityVar: ActivityVar,
) {
    Column {
        QRCode(videoViewModel)
        VideoView(videoViewModel, activityVar)
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun VideoView(
    videoViewModel: VideoViewModel,
    activityVar: ActivityVar
) {
    val context = LocalContext.current

    val onGetConanUrl = { conanUrl: String ->
        activityVar.mySurfaceView?.let { surfaceView ->
            videoViewModel.player.let { player ->
                val ua = WebSettings.getDefaultUserAgent(context)
                SwithunLog.d(ua)

                val headerParams = HeaderParams().apply {
                    setBilibiliReferer()
                }


                player.reset()
                // user-agent 需要用这个设置，header里设置会出现2个 https://blog.csdn.net/xiaoduzi1991/article/details/121968386
                player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "user-agent", "Bilibili Freedoooooom/MarkII")
                player.setDataSource(conanUrl, headerParams.params)
                player.setSurface(surfaceView.holder.surface)
                player.prepareAsync()
                player.start()
            }
        }
    }

    Column {
        Button(onClick = {
            videoViewModel.viewModelScope.launch(Dispatchers.IO) {
                videoViewModel.getConan()?.let { newConanUrl->
                    onGetConanUrl(newConanUrl)
                }
            }
        }
        ) {
            Text(text = "get conna")
        }
        IjkPlayer(player = videoViewModel.player, activityVar)
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun IjkPlayer(player: IjkMediaPlayer, activityVar: ActivityVar) {
    // https://juejin.cn/post/7034363130121551903
    AndroidView(factory = { context ->
        SwithunLog.d("AndroidView # factory")
        val surfaceView = SurfaceView(context)
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                val temp = surfaceView.layoutParams
//                temp.height = ViewGroup.LayoutParams.WRAP_CONTENT
//                temp.width = ViewGroup.LayoutParams.WRAP_CONTENT
//
                temp.height = 600
                temp.width = 900
                surfaceView.layoutParams = temp

//                player.dataSource =
//                    "https://vfx.mtime.cn/Video/2019/03/09/mp4/190309153658147087.mp4"
//                player.setSurface(surfaceView.holder.surface)
//                player.prepareAsync()
//                player.start()
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
            }

        })

        activityVar.mySurfaceView = surfaceView

        surfaceView
    }, update = { surfaceView ->
        SwithunLog.d("AndroidView # update")
    })
}


@Composable
fun QRCode(
    videoVM: VideoViewModel,
) {
    Column(verticalArrangement = Arrangement.Top) {
        Text(text = videoVM.loginStatus)
        Image(painter = BitmapPainter(videoVM.qrCodeImage), contentDescription = "qrCode")
    }
}

@Composable
fun WordsScreen(
    wordsResult: WordsResult,
    sendMessage: (String) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        var textState by remember { mutableStateOf("") }
        val onTextChange = { text: String ->
            textState = text
        }

        OutlinedTextField(
            value = textState,
            onValueChange = { onTextChange(it) },
            singleLine = true,
            label = { Text(text = "Enter message") },
            modifier = Modifier.padding(10.dp),
            textStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "单词：")
            Text(text = wordsResult.word)
        }
        val bullet = "\u2022"
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "解释：")
            Text(text = buildAnnotatedString {
                wordsResult.explains.forEach {
                    withStyle(ParagraphStyle(textIndent = TextIndent(restLine = 12.sp))) {
                        append(bullet)
                        append("\t\t")
                        append(it)
                    }
                }
            })
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "翻译：")
            Text(text = wordsResult.translation)
        }

        Button(onClick = { sendMessage(textState) }) {
            Text(text = "Send Message")
        }
    }
}