package com.example.myapplication

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.model.SectionItem
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.util.AuthChecker
import com.example.myapplication.util.HeaderParams
import com.example.myapplication.viewmodel.*
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

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background,
                ) {
                    ScreenSetup(
                        activityVar.wordsVM,
                        activityVar.videoVM,
                        activityVar
                    )
                }
            }
        }
        AuthChecker.checkWriteExternalStorage(this)
    }
}

class ActivityVar(
    var activity: MainActivity,
    var mySurfaceView: SurfaceView? = null,
    val wordsVM: WordsViewModel = ViewModelProvider(activity, object : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return WordsViewModel() as T
        }

    }).get(WordsViewModel::class.java),
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
    )
)

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun ScreenSetup(
    wordsViewModel: WordsViewModel,
    videoViewModel: VideoViewModel,
    activityVar: ActivityVar,
) {
    Row(modifier = Modifier.horizontalScroll(ScrollState(0), true)) {
        IjkPlayer(player = videoViewModel.player, activityVar)
        VideoScreen(
            videoViewModel,
            activityVar
        )
        WordsScreen(
            wordsResult = wordsViewModel.wordsResult,
        ) { wordsViewModel.sendMessage(it) }
        FTPView(activityVar)
        FileManagerView(pathList = activityVar.fileManagerViewModel.pathList, activityVar)
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun FileManagerView(pathList: List<PathItem>, activityVar: ActivityVar) {
    LazyColumn(modifier = Modifier
        .background(Color(R.color.purple_200))
        .width(Dp(600f))
    ) {
        items(pathList) { path: PathItem ->
            when (path) {
                is PathItem.FileItem -> {
                    FileItemView(path)
                }
                is PathItem.FolderItem -> {
                    FolderItemView(path, activityVar)
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun FolderItemView(folder: PathItem.FolderItem, activityVar: ActivityVar) {
    Card(modifier = Modifier
        .fillMaxSize()
        .clickable {
            SwithunLog.d("click folder: ${folder.name}")
            activityVar.fileManagerViewModel.clickFolder(folder)
        },
        backgroundColor = Color(activityVar.activity.getColor(R.color.teal_200))
    ) {
        Column(Modifier.padding(10.dp)
        ) {
            Row {
                Text(text = "Folder: ")
                Text(text = folder.name)
            }
            SimplePathListView(folder.children, activityVar)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun SimplePathListView(pathList: List<PathItem>, activityVar: ActivityVar) {
    pathList.forEach { path ->
        when (path) {
            is PathItem.FileItem -> {
                FileItemView(path)
            }
            is PathItem.FolderItem -> {
                FolderItemView(path, activityVar)
            }
        }
    }
}

@Composable
fun FileItemView(file: PathItem.FileItem) {
    Card(modifier = Modifier
        .fillMaxSize()
        .clickable {
            SwithunLog.d("click file: ${file.name}")
        }
    ) {
        Row(Modifier.padding(10.dp)) {
            Text(text = "File: ")
            Text(text = file.name)
        }
    }
}

@Composable
fun FTPView(activityVar: ActivityVar) {
    Column {
        Button(onClick = {
            activityVar.ftpVM.initFTP()
        }) {
            Text(text = "start FTP")
        }
        Text(text = activityVar.ftpVM.myIPStr)
        Button(onClick = {
            activityVar.ftpVM.connectFTP(2221)
        }) {
            Text(text = "connect FTP 2221")
        }
        Button(onClick = {
            activityVar.ftpVM.connectFTP(5656)
        }) {
            Text(text = "connect FTP 5656")
        }

        Button(onClick = {
            activityVar.ftpVM.listFTP(2221)
        }) {
            Text(text = "list 2221")
        }
        Button(onClick = {
            activityVar.ftpVM.listFTP(5656)
        }) {
            Text(text = "list 5656")
        }
        Button(onClick = {
            activityVar.ftpVM.viewModelScope.launch(Dispatchers.IO) {
                val url = activityVar.ftpVM.downloadFile(5656)
            }
        }) {
            Text(text = "download file")
        }
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun VideoScreen(
    videoViewModel: VideoViewModel,
    activityVar: ActivityVar,
) {
    Row {
        VideoView(videoViewModel, activityVar)
        QRCode(videoViewModel)
    }
}

private fun play(
    player: IjkMediaPlayer,
    surfaceView: SurfaceView,
    conanUrl: String,
    headerParams: HeaderParams,
    onComplete: () -> Unit
) {
    SwithunLog.d("运行playe")
    player.reset()
    // user-agent 需要用这个设置，否则header里设置会出现2个 https://blog.csdn.net/xiaoduzi1991/article/details/121968386
    player.setOption(
        IjkMediaPlayer.OPT_CATEGORY_FORMAT,
        "user-agent",
        "Bilibili Freedoooooom/MarkII"
    )
    player.setOption(
        IjkMediaPlayer.OPT_CATEGORY_FORMAT,
        "reconnect",
        1
    );//重连模式，如果中途服务器断开了连接，让它重新连接,参考 https://github.com/Bilibili/ijkplayer/issues/445
    player.setOption(
        IjkMediaPlayer.OPT_CATEGORY_FORMAT,
        "dns_cache_clear",
        1
    );// 解决 Hit dns cache but connect fail hostname
    player.setOption(
        IjkMediaPlayer.OPT_CATEGORY_FORMAT,
        "protocol_whitelist",
        "async,cache,crypto,file,http,https,ijkhttphook,ijkinject,ijklivehook,ijklongurl,ijksegment,ijktcphook,pipe,rtp,tcp,tls,udp,ijkurlhook,data,ftp"
    );
    player.setDataSource(conanUrl, headerParams.params)
    player.setSurface(surfaceView.holder.surface)


    player.setOnPreparedListener {
        SwithunLog.d("old prepared")
        player.seekTo(0)
    }

    player.setOnCompletionListener {
        onComplete()
    }


    player.prepareAsync()
    player.start()
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

                try {
                    // 循环播放
                    play(player, surfaceView, conanUrl, headerParams) {
                        playNextConan(videoViewModel, player, surfaceView, headerParams)
                    }
                    // 播放进度计算
                    videoViewModel.viewModelScope.launch {
                        while (true) {
                            delay(500)
                            val duration = when (val duration = player.duration) {
                                0L -> 1F
                                else -> duration.toFloat()
                            }
                            videoViewModel.currentProcess =
                                player.currentPosition.toFloat() / duration
                        }
                    }
                } catch (e: Error) {
                    SwithunLog.e("player err")
                }
            }
        }
    }

    val onGetFTPUrl = { ftpUrl: String ->
        SwithunLog.d("ftpUrl: $ftpUrl")

        activityVar.mySurfaceView?.let { surfaceView ->
            val player = videoViewModel.player

            player.reset()
            // user-agent 需要用这个设置，否则header里设置会出现2个 https://blog.csdn.net/xiaoduzi1991/article/details/121968386
//            player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "user-agent", "Bilibili Freedoooooom/MarkII")
            player.setOption(
                IjkMediaPlayer.OPT_CATEGORY_FORMAT,
                "reconnect",
                1
            );//重连模式，如果中途服务器断开了连接，让它重新连接,参考 https://github.com/Bilibili/ijkplayer/issues/445
            player.setOption(
                IjkMediaPlayer.OPT_CATEGORY_FORMAT,
                "dns_cache_clear",
                1
            );// 解决 Hit dns cache but connect fail hostname
            player.setOption(
                IjkMediaPlayer.OPT_CATEGORY_FORMAT,
                "protocol_whitelist",
                "async,cache,crypto,file,http,https,ijkhttphook,ijkinject,ijklivehook,ijklongurl,ijksegment,ijktcphook,pipe,rtp,tcp,tls,udp,ijkurlhook,data,ftp"
            );
            player.dataSource = ftpUrl
            player.setSurface(surfaceView.holder.surface)

            player.prepareAsync()
            player.start()

        }

    }

    val onGetHttpUr = { httpUrl: String ->
        SwithunLog.d("httpUrl: $httpUrl")

        activityVar.mySurfaceView?.let { surfaceView ->
            val player = videoViewModel.getNewPlayer()

            player.reset()
            player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-hevc", 1)
            player.dataSource = httpUrl
            player.setSurface(surfaceView.holder.surface)

            player.prepareAsync()
            player.start()

        }
    }

    Row {
        Column {
            Button(onClick = {
                videoViewModel.viewModelScope.launch(Dispatchers.IO) {
                    videoViewModel.getConan()?.let { newConanUrl ->
                        onGetConanUrl(newConanUrl)
                    }
                }
            }
            ) {
                Text(text = "get conna")
            }
            Button(onClick = {
                videoViewModel.viewModelScope.launch(Dispatchers.IO) {
                    activityVar.ftpVM.viewModelScope.launch(Dispatchers.IO) {
                        val url = activityVar.ftpVM.downloadFile(2221)
                        onGetFTPUrl(url)
                    }
                }
            }) {
                Text(text = "get FTP")
            }
            Button(onClick = {
                videoViewModel.viewModelScope.launch(Dispatchers.IO) {
                    activityVar.ftpVM.viewModelScope.launch(Dispatchers.IO) {
                        val url = activityVar.ftpVM.downloadFile(5656)
                        onGetFTPUrl(url)
                    }
                }
            }) {
                Text(text = "get FTP 5656")
            }

            Button(onClick = {
                onGetHttpUr(videoViewModel.testGetHttpMp4())
            }) {
                Text(text = "getHttp")
            }

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
                    videoViewModel.viewModelScope.launch {
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
    player: IjkMediaPlayer,
    surfaceView: SurfaceView,
    headerParams: HeaderParams
) {
    videoViewModel.viewModelScope.launch {
        val nextConanUrl =
            videoViewModel.getNextConan().nullCheck("get nextConanUrl") ?: return@launch
        play(player, surfaceView, nextConanUrl, headerParams) {
            playNextConan(videoViewModel, player, surfaceView, headerParams)
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