package com.example.lantian_front.util

import android.annotation.TargetApi
import android.content.Context
import android.net.Uri
import android.os.Build
import android.preference.PreferenceManager
import android.provider.DocumentsContract
import android.text.TextUtils
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.example.lantian_front.util.StorageUtils.getSDCardDir
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


object DocumentsUtils {
    private val TAG = DocumentsUtils::class.java.simpleName
    const val OPEN_DOCUMENT_TREE_CODE = 8000
    private val sExtSdCardPaths: MutableList<String> = ArrayList()
    fun cleanCache() {
        sExtSdCardPaths.clear()
    }

    /**
     * Get a list of external SD card paths. (Kitkat or higher.)
     *
     * @return A list of external SD card paths.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private fun getExtSdCardPaths(context: Context): Array<String> {
        if (sExtSdCardPaths.size > 0) {
            return sExtSdCardPaths.toTypedArray()
        }
        for (file in context.getExternalFilesDirs("external")) {
            if (file != null && file != context.getExternalFilesDir("external")) {
                val index = file.absolutePath.lastIndexOf("/Android/data")
                if (index < 0) {
                    Log.w(TAG, "Unexpected external file dir: " + file.absolutePath)
                } else {
                    var path = file.absolutePath.substring(0, index)
                    try {
                        path = File(path).canonicalPath
                    } catch (e: IOException) {
                        // Keep non-canonical path.
                    }
                    sExtSdCardPaths.add(path)
                }
            }
        }
        if (sExtSdCardPaths.isEmpty()) sExtSdCardPaths.add("/storage/sdcard1")
        return sExtSdCardPaths.toTypedArray()
    }

    /**
     * Determine the main folder of the external SD card containing the given file.
     *
     * @param file the file.
     * @return The main folder of the external SD card containing this file, if the file is on an SD
     * card. Otherwise,
     * null is returned.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private fun getExtSdCardFolder(file: File, context: Context): String? {
        val extSdPaths = getExtSdCardPaths(context)
        try {
            for (i in extSdPaths.indices) {
                if (file.canonicalPath.startsWith(extSdPaths[i])) {
                    return extSdPaths[i]
                }
            }
        } catch (e: IOException) {
            return null
        }
        return null
    }

    /**
     * Determine if a file is on external sd card. (Kitkat or higher.)
     *
     * @param file The file.
     * @return true if on external sd card.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    fun isOnExtSdCard(file: File, c: Context): Boolean {
        return getExtSdCardFolder(file, c) != null
    }

    /**
     * Get a DocumentFile corresponding to the given file (for writing on ExtSdCard on Android 5).
     * If the file is not
     * existing, it is created.
     *
     * @param file        The file.
     * @param isDirectory flag indicating if the file should be a directory.
     * @return The DocumentFile
     */
    fun getDocumentFile(
        file: File, isDirectory: Boolean,
        context: Context,
    ): DocumentFile? {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            return DocumentFile.fromFile(file)
        }
        val baseFolder = getExtSdCardFolder(file, context)
        var originalDirectory = false
        if (baseFolder == null) {
            return null
        }
        var relativePath: String? = null
        try {
            val fullPath = file.canonicalPath
            if (baseFolder != fullPath) {
                relativePath = fullPath.substring(baseFolder.length + 1)
            } else {
                originalDirectory = true
            }
        } catch (e: IOException) {
            return null
        } catch (f: Exception) {
            originalDirectory = true
            //continue
        }
        val `as` = PreferenceManager.getDefaultSharedPreferences(context).getString(
            baseFolder,
            null
        )
        var treeUri: Uri? = null
        if (`as` != null) treeUri = Uri.parse(`as`)
        if (treeUri == null) {
            return null
        }

        // start with root of SD card and then parse through document tree.
        var document = DocumentFile.fromTreeUri(context, treeUri)
        if (originalDirectory) return document
        val parts = relativePath!!.split("/".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        for (i in parts.indices) {
            var nextDocument = document!!.findFile(parts[i])
            if (nextDocument == null) {
                nextDocument = if (i < parts.size - 1 || isDirectory) {
                    document.createDirectory(parts[i])
                } else {
                    document.createFile("image", parts[i])
                }
            }
            document = nextDocument
        }
        return document
    }

    fun mkdirs(context: Context, dir: File): Boolean {
        var res = dir.mkdirs()
        if (!res) {
            if (isOnExtSdCard(dir, context)) {
                val documentFile = getDocumentFile(dir, true, context)
                res = documentFile != null && documentFile.canWrite()
            }
        }
        return res
    }

    fun delete(context: Context, file: File): Boolean {
        var ret = file.delete()
        if (!ret && isOnExtSdCard(file, context)) {
            val f = getDocumentFile(file, false, context)
            if (f != null) {
                ret = f.delete()
            }
        }
        return ret
    }

    fun canWrite(file: File): Boolean {
        var res = file.exists() && file.canWrite()
        if (!res && !file.exists()) {
            try {
                res = if (!file.isDirectory) {
                    file.createNewFile() && file.delete()
                } else {
                    file.mkdirs() && file.delete()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return res
    }

    fun canWrite(context: Context, file: File): Boolean {
        var res = canWrite(file)
        if (!res && isOnExtSdCard(file, context)) {
            val documentFile = getDocumentFile(file, true, context)
            res = documentFile != null && documentFile.canWrite()
        }
        return res
    }

    fun renameTo(context: Context, src: File, dest: File): Boolean {
        var res = src.renameTo(dest)
        if (!res && isOnExtSdCard(dest, context)) {
            val srcDoc: DocumentFile?
            srcDoc = if (isOnExtSdCard(src, context)) {
                getDocumentFile(src, false, context)
            } else {
                DocumentFile.fromFile(src)
            }
            val destDoc = getDocumentFile(dest.parentFile, true, context)
            if (srcDoc != null && destDoc != null) {
                try {
                    if (src.parent == dest.parent) {
                        res = srcDoc.renameTo(dest.name)
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        res = DocumentsContract.moveDocument(
                            context.contentResolver,
                            srcDoc.uri,
                            srcDoc.parentFile!!.uri,
                            destDoc.uri
                        ) != null
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return res
    }

    fun getInputStream(context: Context, destFile: File): InputStream? {
        var `in`: InputStream? = null
        try {
            if (!canWrite(destFile) && isOnExtSdCard(destFile, context)) {
                val file = getDocumentFile(destFile, false, context)
                if (file != null && file.canWrite()) {
                    `in` = context.contentResolver.openInputStream(file.uri)
                }
            } else {
                `in` = FileInputStream(destFile)
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        return `in`
    }

    fun getOutputStream(context: Context, destFile: File): OutputStream? {
        var out: OutputStream? = null
        try {
            if (!canWrite(destFile) && isOnExtSdCard(destFile, context)) {
                val file = getDocumentFile(destFile, false, context)
                if (file != null && file.canWrite()) {
                    out = context.contentResolver.openOutputStream(file.uri)
                }
            } else {
                out = FileOutputStream(destFile)
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        return out
    }

    fun saveTreeUri(context: Context?, rootPath: String, uri: Uri): Boolean {
        val file = DocumentFile.fromTreeUri(context!!, uri)
        Log.d(TAG, "saveTreeUri= $uri")
        if (file != null && file.canWrite()) {
            val perf = PreferenceManager.getDefaultSharedPreferences(context)
            perf.edit().putString(rootPath, uri.toString()).apply()
            return true
        } else {
            Log.e(TAG, "no write permission: $rootPath")
        }
        Log.d(TAG, "save_tree_uri=false")
        return false
    }

    fun getTreeUri(context: Context?, rootPath: String?): String? {
        val perf = PreferenceManager.getDefaultSharedPreferences(context)
        val treeUri = perf.getString(rootPath, "")
        Log.d(TAG, "treeUri=$treeUri")
        return treeUri
    }

    fun checkWritableRootPath(context: Context, rootPath: String?): Boolean {
        val root = File(rootPath)
        return if (!root.canWrite()) {
            if (isOnExtSdCard(root, context)) {
                val documentFile = getDocumentFile(root, true, context)
                documentFile == null || !documentFile.canWrite()
            } else {
                val perf = PreferenceManager.getDefaultSharedPreferences(context)
                val documentUri = perf.getString(rootPath, "")
                if (documentUri == null || documentUri.isEmpty()) {
                    true
                } else {
                    val file = DocumentFile.fromTreeUri(context, Uri.parse(documentUri))
                    !(file != null && file.canWrite())
                }
            }
        } else false
    }

    fun isExist(documentFile: DocumentFile?, fileName: String?): Boolean {
        var exist = false
        if (documentFile != null) {
            val file = documentFile.findFile(fileName!!)
            if (file != null) {
                exist = file.exists()
            }
        } else {
            Log.w(TAG, "documentFile null")
        }
        return exist
    }

    /**
     * 在上一级目录下创建目录
     *
     * @param parentDir 父节点目录 docFile
     * @param dirName   child 目录名
     */
    fun mkdir(parentDir: DocumentFile?, dirName: String): DocumentFile? {
        if (parentDir != null && !TextUtils.isEmpty(dirName)) {
            return if (!isExist(parentDir, dirName)) {
                parentDir.createDirectory(dirName)
            } else {
                parentDir.findFile(dirName)
            }
        }
        Log.d(TAG, "dirName = $dirName")
        return null
    }

    /**
     * @param context      context
     * @param uri          uri 默认传null ,从sp中读取
     * @param orgFilePath  被复制的文件路径
     * @param destFileName 复制后的文件名
     * @desc 将orgFilePath文件复制到指定SD卡指定路径/storage/xx-xx/hello/
     */
    fun doCopyFile(
        context: Context,
        uri: Uri?,
        orgFilePath: String?,
        destFileName: String?,
    ): Boolean {
        // 初始化基础目录，确保录音所在目录存在
        // 获取到根目录的uri
        var uri = uri
        if (uri == null) {
            val sdPath = getSDCardDir(context)
            if (TextUtils.isEmpty(sdPath)) {
                return false
            }
            val uriStr = getTreeUri(context, sdPath)
            if (TextUtils.isEmpty(uriStr)) {
                return false
            }
            uri = Uri.parse(uriStr)
        }
        var destFile: DocumentFile? = null
        val inFile = File(orgFilePath)
        val documentFile = DocumentFile.fromTreeUri(context, uri!!)
        if (documentFile != null) {
            val documentFiles = documentFile.listFiles()
            for (file in documentFiles) {
                Log.i(TAG, "file.getName() = " + file.name)
                if (file.isDirectory && file.name != null && file.name == "hello") {
                    destFile = file
                }
            }
            // 进行文件复制
            if (destFile != null) {
                val newFile = destFile.createFile("*/*", destFileName!!)
                if (newFile != null) {
                    return copyFile(context, inFile, newFile)
                } else {
                    Log.w(TAG, "newFile is null")
                }
            }
        }
        return false
    }

    /**
     * ref: https://www.jianshu.com/p/2f5d80688ca6
     */
    private fun copyFile(context: Context, inFile: File, documentFile: DocumentFile): Boolean {
        var out: OutputStream? = null
        return try {
            val `in`: InputStream = FileInputStream(inFile)
            out = context.contentResolver.openOutputStream(documentFile.uri)
            val buf = ByteArray(1024 * 10)
            var len: Int
            while (`in`.read(buf).also { len = it } > 0) {
                out!!.write(buf, 0, len)
            }
            `in`.close()
            true
        } catch (e: IOException) {
            Log.e(TAG, "error = " + e.message)
            e.printStackTrace()
            false
        } finally {
            if (out != null) {
                try {
                    out.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
}