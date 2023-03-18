package com.example.myapplication

import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.webkit.WebSettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.example.myapplication.model.KernelConfig
import com.example.myapplication.model.SectionItem
import com.example.myapplication.model.ServerConfig
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.view.ServerFilePage
import com.example.myapplication.ui.view.TransferPage
import com.example.myapplication.util.AuthChecker
import com.example.myapplication.util.HeaderParams
import com.example.myapplication.viewmodel.*
import com.swithun.liu.ServerSDK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tv.danmaku.ijk.media.player.IjkMediaPlayer


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
                    color = MaterialTheme.colors.background,
                ) {
                    // ScreenSetup(activityVar)
                    Myapp(activityVar)
                }
            }
        }
        AuthChecker.checkWriteExternalStorage(this)
    }
}

class ActivityVar(
    var activity: MainActivity,
    val kernelConfig: KernelConfig = KernelConfig { activity },
    val serverConfig: ServerConfig = ServerConfig,
    var mySurfaceView: SurfaceView? = null,
    val connectVM: ConnectKernelViewModel = ViewModelProvider(
        activity,
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return ConnectKernelViewModel() as T
            }
        }).get(ConnectKernelViewModel::class.java),
    val connectServerVM: ConnectServerViewModel = ViewModelProvider(
        activity,
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return ConnectServerViewModel() as T
            }

        }).get(ConnectServerViewModel::class.java),
    val videoVM: VideoViewModel = ViewModelProvider(activity, object : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return VideoViewModel { activity } as T
        }
    }).get(VideoViewModel::class.java),
    val ftpVM: FTPViewModel = ViewModelProvider(activity, object : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return FTPViewModel { activity } as T
        }
    }).get(FTPViewModel::class.java),
    val nasVM: NasViewModel = ViewModelProvider(activity, object : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return NasViewModel { activity } as T
        }
    }).get(NasViewModel::class.java),
    val fileManagerViewModel: FileManagerViewModel = ViewModelProvider(activity).get(
        FileManagerViewModel::class.java
    ),
    var scaffoldState: ScaffoldState? = null
) {
    init {
        fileManagerViewModel.init(this)
        connectVM.init(this)
        nasVM.init(this)
        connectServerVM.init(this)
    }
}

enum class PageIndex {
    VIDEO_PAGE,
    VIDEO_LIST_PAGE,
    TRANSFER_PAGE,
    SERVER_SETTING_PAGE,
    SERVER_FILE_PAGE;

    companion object {
        fun fromValue(ordinal: Int) = values().find { it.ordinal == ordinal }
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun Myapp(activityVar: ActivityVar) {

    val (selectedItem: Int, setSelectedItem: (Int) -> Unit) = remember { mutableStateOf(0) }

    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()

    activityVar.scaffoldState = scaffoldState

    Scaffold(
        scaffoldState = scaffoldState,
        content = {
            Row(modifier = Modifier.horizontalScroll(ScrollState(0), true)) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(120.dp)
                        .background(Color.Gray)
                ) {
                    Button(onClick = { setSelectedItem(PageIndex.VIDEO_PAGE.ordinal) }) {
                        Text("视频播放")
                    }
                    Button(onClick = { setSelectedItem(PageIndex.VIDEO_LIST_PAGE.ordinal) }) {
                        Text("视频列表")
                    }
                    Button(onClick = { setSelectedItem(PageIndex.TRANSFER_PAGE.ordinal) }) {
                        Text("传输")
                    }
                    Button(onClick = { setSelectedItem(PageIndex.SERVER_SETTING_PAGE.ordinal) }) {
                        Text("服务器设置")
                    }
                    Button(onClick = { setSelectedItem(PageIndex.SERVER_FILE_PAGE.ordinal) }) {
                        Text("服务器文件")
                    }
                }
                when (PageIndex.fromValue(selectedItem)) {
                    PageIndex.VIDEO_PAGE -> VideoPage(activityVar)
                    PageIndex.VIDEO_LIST_PAGE -> VideoListPage(activityVar)
                    PageIndex.TRANSFER_PAGE -> TransferPage(activityVar)
                    PageIndex.SERVER_SETTING_PAGE -> ServerSettingPage(activityVar)
                    PageIndex.SERVER_FILE_PAGE -> ServerFilePage(activityVar)
                    null -> {}
                }
            }
        })
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun VideoListPage(activityVar: ActivityVar) {
    Row {
        VideoView(activityVar)
        QRCode(activityVar.videoVM)
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun VideoPage(activityVar: ActivityVar) {
    IjkPlayer(player = activityVar.videoVM.player, activityVar)
}


@Composable
fun ServerSettingPage(activityVar: ActivityVar) {
    Row(modifier = Modifier.fillMaxHeight()) {
        Column {
            Text(text = activityVar.ftpVM.myIPStr)
            Row {
                Button(onClick = {
                    activityVar.activity.lifecycleScope.launch(Dispatchers.IO) {
                        SwithunLog.d("begin get ips")
                        val ips = activityVar.nasVM.searchAllServer()
                        SwithunLog.d(ips)
                    }
                }) {
                    Text(text = activityVar.nasVM.getAllServerBtnText)
                }
                ServerList(activityVar)
            }
        }

        Box(
            modifier = Modifier
                .width(10.dp)
                .fillMaxHeight()
                .padding(2.dp)
                .background(Color.Black)
        )
        Button(onClick = {
            activityVar.nasVM.startMeAsServer()
        }) {
            Text(text = activityVar.nasVM.startMeAsServerBtnText)
        }
    }
}

@Composable
fun ServerList(activityVar: ActivityVar) {
    LazyColumn(modifier = Modifier.width(Dp(200f))) {
        items(activityVar.nasVM.allServersInLan) { serverIp: String ->
            Button(onClick = {
                activityVar.connectServerVM.connectServer(serverIp)
            }) {
                Text(text = "连接 $serverIp")
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun VideoView(activityVar: ActivityVar) {
    val videoViewModel = activityVar.videoVM

    val onGetConanUrl = handler@{ conanUrl: String ->
        val surfaceView = activityVar.mySurfaceView ?: return@handler
        val headerParams = HeaderParams().apply { setBilibiliReferer() }
        try {
            // 循环播放
            videoViewModel.play(surfaceView, conanUrl, headerParams) {
                playNextConan(videoViewModel, surfaceView, headerParams)
            }
            // 播放进度计算
            videoViewModel.viewModelScope.launch {
                while (true) {
                    delay(500)
                    val duration = when (val duration = videoViewModel.player.duration) {
                        0L -> 1F
                        else -> duration.toFloat()
                    }
                    videoViewModel.currentProcess =
                        videoViewModel.player.currentPosition.toFloat() / duration
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
            Text(text = videoViewModel.currentProcess.toString())
        }

        LazyColumn(
            modifier = Modifier
                .background(Color(R.color.purple_200))
                .width(Dp(100f))
        ) {
            items(videoViewModel.itemList) { sectionItem: SectionItem ->
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

fun playNextConan(
    videoViewModel: VideoViewModel,
    surfaceView: SurfaceView,
    headerParams: HeaderParams
) {
    videoViewModel.viewModelScope.launch {
        val nextConanUrl =
            videoViewModel.getNextConan().nullCheck("get nextConanUrl") ?: return@launch
        videoViewModel.play(surfaceView, nextConanUrl, headerParams) {
            playNextConan(videoViewModel, surfaceView, headerParams)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun IjkPlayer(player: IjkMediaPlayer, activityVar: ActivityVar) {
    // https://juejin.cn/post/7034363130121551903
    AndroidView(
        factory = { context ->
            SwithunLog.d("AndroidView # factory")
            val surfaceView = SurfaceView(context)
            surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    val temp = surfaceView.layoutParams
                    temp.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    temp.width = ViewGroup.LayoutParams.WRAP_CONTENT
                    surfaceView.layoutParams = temp
                    // surfaceView在activity Stop时会destroy，重新切到前台会重新走create，这里要重新setDisplay
                    // 否则会黑屏但是有声音 https://github.com/Bilibili/ijkplayer/issues/2666#issuecomment-800083756
                    player.setDisplay(holder)
                    player.start()
                }

                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) {
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    player.pause()
                }

            })

            activityVar.mySurfaceView = surfaceView

            surfaceView
        },
        modifier = Modifier.width(Dp(1000f)),
        update = {
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
fun WordsScreen(activityVar: ActivityVar) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(400.dp)
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

        Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = "单词：")
            Text(text = activityVar.connectServerVM.wordsResult.word)
        }
        val bullet = "\u2022"
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = "解释：")
            Text(text = buildAnnotatedString {
                activityVar.connectServerVM.wordsResult.explains.forEach {
                    withStyle(ParagraphStyle(textIndent = TextIndent(restLine = 12.sp))) {
                        append(bullet)
                        append("\t\t")
                        append(it)
                    }
                }
            })
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = "翻译：")
            Text(text = activityVar.connectServerVM.wordsResult.translation)
        }

        Button(onClick = { activityVar.connectServerVM.sendMessage(textState) }) {
            Text(text = "Send Message")
        }
    }
}