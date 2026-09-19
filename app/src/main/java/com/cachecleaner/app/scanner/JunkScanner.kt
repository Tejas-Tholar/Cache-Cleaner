package com.cachecleaner.app.scanner

import android.content.Context
import android.os.Environment
import com.cachecleaner.app.model.JunkCategory
import com.cachecleaner.app.model.JunkItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class JunkScanner(private val context: Context) {

    /**
     * Scans shared storage locations for generic temporary and cache files.
     */
    suspend fun scanJunkFiles(
        onProgress: ((currentDir: String) -> Unit)? = null
    ): List<JunkItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<JunkItem>()
        val externalStorage = Environment.getExternalStorageDirectory() ?: return@withContext emptyList()

        val candidateDirs = listOf(
            File(externalStorage, "Download"),
            File(externalStorage, "Android/data"),
            File(externalStorage, "DCIM/.thumbnails"),
            File(externalStorage, "Pictures/.thumbnails"),
            File(externalStorage, "WhatsApp/Media/.Statuses")
        )

        for (dir in candidateDirs) {
            if (dir.exists() && dir.canRead()) {
                onProgress?.invoke(dir.name)
                scanDirectory(dir, results, maxDepth = 3, currentDepth = 0)
            }
        }

        results
    }

    private fun scanDirectory(dir: File, results: MutableList<JunkItem>, maxDepth: Int, currentDepth: Int) {
        if (currentDepth > maxDepth) return
        val files = dir.listFiles() ?: return

        for (file in files) {
            if (file.isDirectory) {
                // Check for empty directory
                val children = file.listFiles()
                if (children != null && children.isEmpty()) {
                    results.add(JunkItem(file, JunkCategory.EMPTY_DIRECTORIES, 0L))
                } else {
                    scanDirectory(file, results, maxDepth, currentDepth + 1)
                }
            } else {
                val nameLower = file.name.lowercase()
                when {
                    nameLower.endsWith(".tmp") || nameLower.endsWith(".temp") || nameLower.startsWith("cache_") -> {
                        results.add(JunkItem(file, JunkCategory.TEMP_FILES, file.length()))
                    }
                    nameLower.endsWith(".log") -> {
                        results.add(JunkItem(file, JunkCategory.LOG_FILES, file.length()))
                    }
                    nameLower.endsWith(".thumb") || file.parent?.contains(".thumbnails") == true -> {
                        results.add(JunkItem(file, JunkCategory.THUMBNAIL_CACHE, file.length()))
                    }
                    nameLower.endsWith(".apk") && file.parent?.contains("Download") == true -> {
                        results.add(JunkItem(file, JunkCategory.APK_REMNANTS, file.length()))
                    }
                }
            }
        }
    }

    /**
     * Deletes the selected junk files
     */
    suspend fun cleanJunkItems(items: List<JunkItem>): Pair<Int, Long> = withContext(Dispatchers.IO) {
        var count = 0
        var bytesFreed = 0L
        for (item in items.filter { it.isSelected }) {
            val size = item.sizeBytes
            if (item.file.delete()) {
                count++
                bytesFreed += size
            }
        }
        Pair(count, bytesFreed)
    }
}
