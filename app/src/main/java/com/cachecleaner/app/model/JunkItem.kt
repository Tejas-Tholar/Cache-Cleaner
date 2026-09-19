package com.cachecleaner.app.model

import java.io.File

enum class JunkCategory {
    APP_CACHE,
    TEMP_FILES,
    LOG_FILES,
    THUMBNAIL_CACHE,
    EMPTY_DIRECTORIES,
    APK_REMNANTS
}

data class JunkItem(
    val file: File,
    val category: JunkCategory,
    val sizeBytes: Long,
    var isSelected: Boolean = true
) {
    val name: String get() = file.name
    val path: String get() = file.absolutePath
    fun formattedSize(): String = AppInfo.formatBytes(sizeBytes)
}
