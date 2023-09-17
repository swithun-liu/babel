package com.example.lantian_front.viewmodel.filemanager

import com.example.lantian_front.framework.BaseViewModel
import com.example.lantian_front.model.MessageTextDTO
import com.example.lantian_front.model.Storage
import com.example.lantian_front.viewmodel.filemanager.model.PathItem

sealed class Action : BaseViewModel.AAction() {
    class ClickFolder(val folder: PathItem.FolderItem) : Action()
    class ClickFile(val file: PathItem.FileItem) : Action()
    object RefreshBasePathListFromRemote : Action()
    class FindUsbFile(val path: String, val uuid: String) : Action()
    class GetUsbFileByPiece(val data: MessageTextDTO) : Action()
    object GetStorageList : Action()
    class GetFileListOfStorage(val s: Storage, val filePath: String) : Action()
    class ClickFolderV2(val folder: PathItem.FolderItem) : Action()
    class ClickFileV2(val file: PathItem.FileItem) : Action()
}