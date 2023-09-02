package com.example.lantian_front.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

object SystemUtil {
    fun pushText2Clipboard(context: Context, text: String) {
        val clipboard =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("text/plain", text)
        clipboard.setPrimaryClip(clip)
    }

    fun getTextFromClipboard(context: Context): String {
        val clipboard =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
    }
}