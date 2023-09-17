package com.example.lantian_front.viewmodel.filemanager.model

import com.example.lantian_front.model.FileItem
import com.example.lantian_front.model.Storage

sealed class PathItem(open val name: String, open val path: String, open val storage: Storage) {

    data class FolderItem(
        override val name: String,
        override val path: String,
        override val storage: Storage,
        var children: List<PathItem>,
        var isOpening: Boolean = false
    ) : PathItem(name, path, storage) {

        companion object {
            fun getPreview() = FolderItem(
                "name", "path/name", Storage.getPreview(), emptyList()
            )
        }
    }

    data class FileItem(
        override val name: String,
        override val path: String,
        override val storage: Storage,
    ) : PathItem(name, path, storage) {
        companion object {
            fun getPreview(): FileItem {
                return FileItem(
                    "name", "path/name", Storage.getPreview()
                )
            }
        }
    }
}