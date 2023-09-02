package com.example.lantian_front.util

import android.content.Context
import android.os.StatFs
import android.os.storage.StorageManager
import android.util.Log
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.text.DecimalFormat


object StorageUtils {
    private const val TAG = "StorageUtils"

    //定义GB的计算常量
    private const val GB = 1024 * 1024 * 1024

    //定义MB的计算常量
    private const val MB = 1024 * 1024

    //定义KB的计算常量
    private const val KB = 1024
    fun bytes2kb(bytes: Long): String {
        //格式化小数
        val format = DecimalFormat("###.0")
        return if (bytes / GB >= 1) {
            format.format((bytes * 1.0f / GB).toDouble()) + "GB"
        } else if (bytes / MB >= 1) {
            format.format((bytes * 1.0f / MB).toDouble()) + "MB"
        } else if (bytes / KB >= 1) {
            format.format((bytes * 1.0f / KB).toDouble()) + "KB"
        } else {
            bytes.toString() + "B"
        }
    }

    /*
    获取全部存储设备信息封装对象
     */
    fun getVolume(context: Context): ArrayList<Volume> {
        val list_storagevolume = ArrayList<Volume>()
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        try {
            val method_volumeList = StorageManager::class.java.getMethod("getVolumeList")
            method_volumeList.isAccessible = true
            val volumeList = method_volumeList.invoke(storageManager) as Array<Any>
            if (volumeList != null) {
                var volume: Volume
                for (i in volumeList.indices) {
                    try {
                        volume = Volume()
                        volume.path = volumeList[i].javaClass.getMethod("getPath")
                            .invoke(volumeList[i]) as String
                        volume.isRemovable = volumeList[i].javaClass.getMethod("isRemovable")
                            .invoke(volumeList[i]) as Boolean
                        volume.state = volumeList[i].javaClass.getMethod("getState")
                            .invoke(volumeList[i]) as String
                        list_storagevolume.add(volume)
                    } catch (e: IllegalAccessException) {
                        e.printStackTrace()
                    } catch (e: InvocationTargetException) {
                        e.printStackTrace()
                    } catch (e: NoSuchMethodException) {
                        e.printStackTrace()
                    }
                }
            } else {
                Log.e("null", "-null-")
            }
        } catch (e1: Exception) {
            e1.printStackTrace()
        }
        return list_storagevolume
    }

    /**
     * SD卡剩余空间
     */
    fun getSDFreeSize(path: String?): Long {
        try {
            // 取得SD卡文件路径
            val sf = StatFs(path)
            // 获取单个数据块的大小(Byte)
            val blockSize = sf.blockSizeLong
            // 空闲的数据块的数量
            val freeBlocks = sf.availableBlocksLong
            // 返回SD卡空闲大小
            // return freeBlocks * blockSize; //单位Byte
            // return (freeBlocks * blockSize)/1024; //单位KB
            return freeBlocks * blockSize // 单位GB
        } catch (e: IllegalArgumentException) {
            Log.d(TAG, "hello world")
        }
        return 0L
    }

    /**
     * SD卡总容量
     */
    fun getSDAllSize(path: String?): Long {
        try { // 取得SD卡文件路径
            val sf = StatFs(path)
            // 获取单个数据块的大小(Byte)
            val blockSize = sf.blockSizeLong
            // 获取所有数据块数
            val allBlocks = sf.blockCountLong
            // 返回SD卡大小
            // return allBlocks * blockSize; //单位Byte
            // return (allBlocks * blockSize)/1024; //单位KB
            return allBlocks * blockSize // 单位GB
        } catch (e: IllegalArgumentException) {
            Log.d(TAG, "hello world")
        }
        return 0L
    }

    fun getSDCardDir(context: Context): String? {
        var sdcardDir: String? = null
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        var volumeInfoClazz: Class<*>? = null
        var diskInfoClazz: Class<*>? = null
        try {
            diskInfoClazz = Class.forName("android.os.storage.DiskInfo")
            val isSd = diskInfoClazz.getMethod("isSd")
            volumeInfoClazz = Class.forName("android.os.storage.VolumeInfo")
            val getType = volumeInfoClazz.getMethod("getType")
            val getDisk = volumeInfoClazz.getMethod("getDisk")
            val path = volumeInfoClazz.getDeclaredField("path")
            val getVolumes = storageManager.javaClass.getMethod("getVolumes")
            val result = getVolumes.invoke(storageManager) as List<Class<*>>
            for (i in result.indices) {
                val volumeInfo: Any = result[i]
                if (getType.invoke(volumeInfo) as Int == 0) {
                    val disk = getDisk.invoke(volumeInfo)
                    if (disk != null) {
                        if (isSd.invoke(disk) as Boolean) {
                            sdcardDir = path[volumeInfo] as String
                            break
                        }
                    }
                }
            }
            return if (sdcardDir == null) {
                Log.w(TAG, "sdcardDir null")
                null
            } else {
                Log.i(TAG, "sdcardDir " + sdcardDir + File.separator)
                sdcardDir + File.separator
            }
        } catch (e: Exception) {
            Log.i(TAG, "sdcardDir e " + e.message)
            e.printStackTrace()
        }
        Log.w(TAG, "sdcardDir null")
        return null
    }

    fun getUsbDir(context: Context): String? {
        var sdcardDir: String? = null
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        var volumeInfoClazz: Class<*>? = null
        var diskInfoClazz: Class<*>? = null
        try {
            diskInfoClazz = Class.forName("android.os.storage.DiskInfo")
            val isUsb = diskInfoClazz.getMethod("isUsb")
            volumeInfoClazz = Class.forName("android.os.storage.VolumeInfo")
            val getType = volumeInfoClazz.getMethod("getType")
            val getDisk = volumeInfoClazz.getMethod("getDisk")
            val path = volumeInfoClazz.getDeclaredField("path")
            val getVolumes = storageManager.javaClass.getMethod("getVolumes")
            val result = getVolumes.invoke(storageManager) as List<Class<*>>
            for (i in result.indices) {
                val volumeInfo: Any = result[i]
                Log.w(TAG, "disk path " + path[volumeInfo])
                if (getType.invoke(volumeInfo) as Int == 0) {
                    val disk = getDisk.invoke(volumeInfo)
                    Log.w(TAG, "is usb " + isUsb.invoke(disk))
                    if (disk != null) {
                        if (isUsb.invoke(disk) as Boolean) {
                            sdcardDir = path[volumeInfo] as String
                            break
                        }
                    }
                }
            }
            return if (sdcardDir == null) {
                Log.w(TAG, "usbpath null")
                null
            } else {
                Log.i(TAG, "usbpath " + sdcardDir + File.separator)
                sdcardDir + File.separator
            }
        } catch (e: Exception) {
            Log.i(TAG, "usbpath e " + e.message)
            e.printStackTrace()
        }
        Log.w(TAG, "usbpath null")
        return null
    }

    /*
     存储设备信息封装类
     */
    class Volume {
        var path: String? = null
        var isRemovable = false
        var state: String? = null
    }
}