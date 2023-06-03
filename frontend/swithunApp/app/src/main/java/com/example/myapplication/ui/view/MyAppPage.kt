package com.example.myapplication.ui.view

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.view.*
import com.example.myapplication.viewmodel.ShareViewModel


@RequiresApi(Build.VERSION_CODES.M)
@Preview
@Composable
fun PreviewMyApp() {
    Myapp()
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun Myapp(
    shareViewModel: ShareViewModel = viewModel(),
) {
    val (selectedItem: Int, setSelectedItem: (Int) -> Unit) = remember { mutableStateOf(0) }
    val items: List<String> = PageIndex.values().map { it.text }
    val icons: List<ImageVector> = listOf(
        Icons.Filled.Home,
        Icons.Filled.Search,
        Icons.Filled.Settings,
        Icons.Filled.Settings,
        Icons.Filled.Settings,
        Icons.Filled.Settings
    )

    shareViewModel.snackbarHostState

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = shareViewModel.snackbarHostState) },
        content = {
            Row(modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(ScrollState(0), true)) {
                NavigationRail(
                ) {
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
                    PageIndex.VIDEO_PAGE -> VideoPage()
                    PageIndex.VIDEO_LIST_PAGE -> VideoListPage()
                    PageIndex.TRANSFER_PAGE -> TransferPage()
                    PageIndex.SERVER_SETTING_PAGE -> ServerSettingPage()
                    PageIndex.SERVER_FILE_PAGE -> ServerFilePage()
                    PageIndex.FILE_UPLOAD_PAGE -> FileUploadPage()
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
    SERVER_FILE_PAGE("server file"),
    FILE_UPLOAD_PAGE("file upload page")
    ;

    companion object {
        fun fromValue(ordinal: Int) = values().find { it.ordinal == ordinal }
    }
}