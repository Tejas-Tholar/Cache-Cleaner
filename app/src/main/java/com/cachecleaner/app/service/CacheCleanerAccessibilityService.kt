package com.cachecleaner.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class CacheCleanerAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var isProcessingStep = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "CacheCleanerAccessibilityService connected.")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!CleaningManager.isRunning || isProcessingStep) return
        val currentPackage = CleaningManager.currentCleaningPackage ?: return

        val rootNode = rootInActiveWindow ?: return

        // Step 1: Check if we are on the Storage & Cache screen (contains "Clear cache")
        val clearCacheNode = findNodeByText(rootNode, listOf(
            "clear cache", "clear the cache", "löschen", "cache leeren", 
            "vider le cache", "borrar caché", "limpar cache", "svuota cache"
        )) ?: findNodeById(rootNode, listOf(
            "com.android.settings:id/button2",
            "com.android.settings:id/clear_cache_button",
            "clear_cache_button",
            "com.samsung.android.settings:id/clear_cache_button",
            "com.miui.securitycenter:id/clear_cache",
            "com.coloros.safecenter:id/clear_cache"
        ))

        if (clearCacheNode != null) {
            if (clearCacheNode.isEnabled) {
                isProcessingStep = true
                handler.postDelayed({
                    clickNode(clearCacheNode)
                    CleaningManager.recordAppCleaned(currentPackage)
                    Log.d(TAG, "Successfully clicked Clear Cache for: $currentPackage")

                    // Give system time to execute deletion, then proceed to next app
                    handler.postDelayed({
                        isProcessingStep = false
                        CleaningManager.proceedToNextApp(this)
                    }, 450)
                }, 300)
            } else {
                // Button disabled means cache is already 0 B
                CleaningManager.recordAppCleaned(currentPackage)
                CleaningManager.proceedToNextApp(this)
            }
            return
        }

        // Step 2: If we are on the App Info screen, find the "Storage" / "Storage & cache" option
        val storageNode = findNodeByText(rootNode, listOf(
            "storage & cache", "storage usage", "app storage", "storage", 
            "speicher", "stockage", "almacenamiento", "memoria"
        )) ?: findNodeById(rootNode, listOf(
            "com.android.settings:id/storage_settings",
            "com.samsung.android.settings:id/storage_settings",
            "com.android.settings:id/entity_header_content"
        ))

        if (storageNode != null) {
            isProcessingStep = true
            handler.postDelayed({
                clickNode(storageNode)
                isProcessingStep = false
            }, 300)
        }
    }

    private fun findNodeByText(root: AccessibilityNodeInfo, textVariations: List<String>): AccessibilityNodeInfo? {
        for (text in textVariations) {
            val nodes = root.findAccessibilityNodeInfosByText(text)
            if (!nodes.isNullOrEmpty()) {
                for (node in nodes) {
                    if (node.isClickable) return node
                    // check parent if clickable
                    val parent = node.parent
                    if (parent != null && parent.isClickable) return parent
                }
            }
        }
        return null
    }

    private fun findNodeById(root: AccessibilityNodeInfo, idVariations: List<String>): AccessibilityNodeInfo? {
        for (viewId in idVariations) {
            val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
            if (!nodes.isNullOrEmpty()) {
                for (node in nodes) {
                    if (node.isClickable) return node
                    val parent = node.parent
                    if (parent != null && parent.isClickable) return parent
                }
            }
        }
        return null
    }

    private fun clickNode(node: AccessibilityNodeInfo): Boolean {
        return if (node.isClickable) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable) {
                    return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                parent = parent.parent
            }
            false
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility Service Interrupted")
        CleaningManager.stopCleaning()
    }

    companion object {
        private const val TAG = "CacheCleanerService"
        var instance: CacheCleanerAccessibilityService? = null
            private set

        fun isServiceEnabled(): Boolean = instance != null
    }
}
