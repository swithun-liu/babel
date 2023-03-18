package com.example.myapplication

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.model.ActivityVar
import com.example.myapplication.ui.view.*

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun Myapp(activityVar: ActivityVar) {

    var (selectedItem: Int, setSelectedItem: (Int) -> Unit) = remember { mutableStateOf(0) }
    val items: List<String> = PageIndex.values().map { it.text }
    val icons: List<ImageVector> = listOf(Icons.Filled.Home, Icons.Filled.Search, Icons.Filled.Settings, Icons.Filled.Settings, Icons.Filled.Settings)

    val snackbarHostState = rememberScaffoldState()

    Scaffold(
        scaffoldState = snackbarHostState,
        content = {
            Row(modifier = Modifier.horizontalScroll(ScrollState(0), true)) {
                NavigationRail {
                    items.forEachIndexed { index: Int, item: String ->
                        NavigationRailItem(
                            selected = selectedItem == index,
                            onClick = {
                                setSelectedItem(index)
                            },
                            icon = { Icon(imageVector = icons[index], contentDescription = item) },
                            label = { Text(text = item, fontSize = 7.sp) }
                        )
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

enum class PageIndex(val text: String) {
    VIDEO_PAGE("video"),
    VIDEO_LIST_PAGE("video list"),
    TRANSFER_PAGE("transfer"),
    SERVER_SETTING_PAGE("server setting"),
    SERVER_FILE_PAGE("server file");

    companion object {
        fun fromValue(ordinal: Int) = values().find { it.ordinal == ordinal }
    }
}