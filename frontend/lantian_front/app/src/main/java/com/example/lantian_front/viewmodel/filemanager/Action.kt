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
    class GetBaseFileListOfStorage(val s: Storage) : Action()
}