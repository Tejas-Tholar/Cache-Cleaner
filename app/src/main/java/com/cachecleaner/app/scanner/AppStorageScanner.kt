package com.cachecleaner.app.scanner

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.storage.StorageManager
import com.cachecleaner.app.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class AppStorageScanner(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val storageStatsManager: StorageStatsManager? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
        } else null
    }

    /**
     * Checks if the app has been granted the PACKAGE_USAGE_STATS permission
     */
    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Scans installed applications and queries their cache & storage sizes.
     */
    /**
     * Scans installed applications and queries their cache & storage sizes.
     */
    suspend fun scanInstalledApps(
        includeSystemApps: Boolean = false,
        onProgress: ((current: Int, total: Int, currentAppName: String) -> Unit)? = null
    ): List<AppInfo> = withContext(Dispatchers.IO) {
        val appMap = mutableMapOf<String, ApplicationInfo>()

        // 1. Primary discovery: Query all launcher apps (Works 100% reliably across Android 11-15)
        try {
            val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = packageManager.queryIntentActivities(launcherIntent, 0)
            for (info in resolveInfos) {
                val pkgName = info.activityInfo?.packageName ?: continue
                if (pkgName == context.packageName) continue
                val appInfo = info.activityInfo.applicationInfo 
                    ?: try { packageManager.getApplicationInfo(pkgName, 0) } catch (_: Exception) { null }
                if (appInfo != null) {
                    appMap[pkgName] = appInfo
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AppStorageScanner", "Error querying launcher intents", e)
        }

        // 2. Secondary discovery: getInstalledPackages
        try {
            val installedPackages = packageManager.getInstalledPackages(0)
            for (pkg in installedPackages) {
                val appInfo = pkg.applicationInfo ?: continue
                if (appInfo.packageName == context.packageName) continue
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isUpdatedSystem = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

                if (includeSystemApps || !isSystem || isUpdatedSystem || appMap.containsKey(appInfo.packageName)) {
                    appMap[appInfo.packageName] = appInfo
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AppStorageScanner", "Error getInstalledPackages", e)
        }

        // 3. Fallback discovery: getInstalledApplications
        try {
            val installedApps = packageManager.getInstalledApplications(0)
            for (appInfo in installedApps) {
                if (appInfo.packageName == context.packageName) continue
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isUpdatedSystem = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

                if (includeSystemApps || !isSystem || isUpdatedSystem || appMap.containsKey(appInfo.packageName)) {
                    appMap[appInfo.packageName] = appInfo
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AppStorageScanner", "Error querying installed applications", e)
        }

        val results = mutableListOf<AppInfo>()
        val total = appMap.size
        val hasStatsPermission = hasUsageStatsPermission()
        val userHandle: UserHandle = Process.myUserHandle()
        val storageUuid: UUID = StorageManager.UUID_DEFAULT

        for ((index, entry) in appMap.values.withIndex()) {
            val app = entry
            val appLabel = try {
                packageManager.getApplicationLabel(app).toString()
            } catch (_: Exception) {
                app.packageName
            }

            onProgress?.invoke(index + 1, total, appLabel)

            var cacheSize = 0L
            var dataSize = 0L
            var codeSize = 0L

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && storageStatsManager != null && hasStatsPermission) {
                try {
                    val appUuid = app.storageUuid
                    val stats = storageStatsManager!!.queryStatsForPackage(appUuid, app.packageName, userHandle)
                    cacheSize = stats.cacheBytes
                    dataSize = stats.dataBytes
                    codeSize = stats.appBytes
                } catch (e: Exception) {
                    try {
                        val stats = storageStatsManager!!.queryStatsForPackage(StorageManager.UUID_DEFAULT, app.packageName, userHandle)
                        cacheSize = stats.cacheBytes
                        dataSize = stats.dataBytes
                        codeSize = stats.appBytes
                    } catch (_: Exception) {
                        cacheSize = getExternalCacheSize(app.packageName)
                    }
                }
            } else {
                cacheSize = getExternalCacheSize(app.packageName)
            }

            val icon = try {
                packageManager.getApplicationIcon(app)
            } catch (_: Exception) {
                null
            }

            val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            results.add(
                AppInfo(
                    packageName = app.packageName,
                    appName = appLabel,
                    icon = icon,
                    cacheSizeBytes = cacheSize,
                    dataSizeBytes = dataSize,
                    codeSizeBytes = codeSize,
                    isSystemApp = isSystem,
                    isSelected = true // Select all by default so user sees their apps
                )
            )
        }

        // Sort by cache size descending, then by name
        results.sortWith(compareByDescending<AppInfo> { it.cacheSizeBytes }.thenBy { it.appName.lowercase() })
        results
    }

    /**
     * Fallback calculation for external cache folder
     */
    private fun getExternalCacheSize(packageName: String): Long {
        return try {
            val extCache = File("/sdcard/Android/data/$packageName/cache")
            if (extCache.exists()) getFolderSize(extCache) else 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun getFolderSize(dir: File): Long {
        var size = 0L
        val files = dir.listFiles() ?: return 0L
        for (file in files) {
            size += if (file.isDirectory) getFolderSize(file) else file.length()
        }
        return size
    }
}
