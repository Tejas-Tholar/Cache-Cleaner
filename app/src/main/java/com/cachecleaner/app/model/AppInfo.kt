package com.cachecleaner.app.model

import android.graphics.drawable.Drawable
import java.text.DecimalFormat

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    var cacheSizeBytes: Long = 0L,
    var dataSizeBytes: Long = 0L,
    var codeSizeBytes: Long = 0L,
    val isSystemApp: Boolean = false,
    var isSelected: Boolean = true
) {
    val totalStorageBytes: Long
        get() = cacheSizeBytes + dataSizeBytes + codeSizeBytes

    fun formattedCacheSize(): String = formatBytes(cacheSizeBytes)
    fun formattedTotalSize(): String = formatBytes(totalStorageBytes)

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            val df = DecimalFormat("#,##0.#")
            return "${df.format(bytes / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
        }
    }
}
