package com.cachecleaner.app.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.cachecleaner.app.model.AppInfo
import java.util.LinkedList
import java.util.Queue

object CleaningManager {

    var isRunning: Boolean = false
        private set

    var currentCleaningPackage: String? = null
        private set

    private val queue: Queue<AppInfo> = LinkedList()
    private var totalToClean: Int = 0
    private var cleanedCount: Int = 0
    private var cleanedBytes: Long = 0L

    var onProgressUpdate: ((cleaned: Int, total: Int, currentApp: AppInfo?) -> Unit)? = null
    var onCleaningFinished: ((cleanedCount: Int, cleanedBytes: Long) -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    fun startCleaning(context: Context, apps: List<AppInfo>) {
        if (apps.isEmpty()) return
        queue.clear()
        queue.addAll(apps)
        totalToClean = apps.size
        cleanedCount = 0
        cleanedBytes = 0L
        isRunning = true

        proceedToNextApp(context)
    }

    fun proceedToNextApp(context: Context) {
        if (!isRunning) return

        val nextApp = queue.poll()
        if (nextApp == null) {
            // Finished!
            stopCleaning()
            mainHandler.post {
                onCleaningFinished?.invoke(cleanedCount, cleanedBytes)
            }
            return
        }

        currentCleaningPackage = nextApp.packageName
        mainHandler.post {
            onProgressUpdate?.invoke(cleanedCount, totalToClean, nextApp)
        }

        // Open App Info settings screen
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${nextApp.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        context.startActivity(intent)
    }

    fun recordAppCleaned(packageName: String) {
        cleanedCount++
        // Trigger UI callback
        mainHandler.post {
            onProgressUpdate?.invoke(cleanedCount, totalToClean, null)
        }
    }

    fun stopCleaning() {
        isRunning = false
        currentCleaningPackage = null
        queue.clear()
    }
}
