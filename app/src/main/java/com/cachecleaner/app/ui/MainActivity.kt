package com.cachecleaner.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.cachecleaner.app.R
import com.cachecleaner.app.model.AppInfo
import com.cachecleaner.app.scanner.AppStorageScanner
import com.cachecleaner.app.service.CacheCleanerAccessibilityService
import com.cachecleaner.app.service.CleaningManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var scanner: AppStorageScanner
    private lateinit var adapter: AppAdapter
    private val appList = mutableListOf<AppInfo>()

    private lateinit var tvTotalCacheHeader: TextView
    private lateinit var tvAppCount: TextView
    private lateinit var tvSelectedCacheSize: TextView
    private lateinit var progressBarCache: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvApps: RecyclerView
    private lateinit var btnToggleSelectAll: MaterialButton
    private lateinit var btnSort: MaterialButton
    private lateinit var btnAutoClean: MaterialButton

    private var isSortDescending = true
    private var isAllSelected = true
    private var progressDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        scanner = AppStorageScanner(this)
        initViews()
        setupListeners()

        checkPermissionsAndScan()
    }

    override fun onResume() {
        super.onResume()
        // Always refresh on return from settings
        startScan()
    }

    private fun initViews() {
        tvTotalCacheHeader = findViewById(R.id.tvTotalCacheHeader)
        tvAppCount = findViewById(R.id.tvAppCount)
        tvSelectedCacheSize = findViewById(R.id.tvSelectedCacheSize)
        progressBarCache = findViewById(R.id.progressBarCache)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        rvApps = findViewById(R.id.rvApps)
        btnToggleSelectAll = findViewById(R.id.btnToggleSelectAll)
        btnSort = findViewById(R.id.btnSort)
        btnAutoClean = findViewById(R.id.btnAutoClean)

        adapter = AppAdapter(mutableListOf()) {
            updateSummaryCard()
        }
        rvApps.layoutManager = LinearLayoutManager(this)
        rvApps.adapter = adapter
    }

    private fun setupListeners() {
        swipeRefresh.setOnRefreshListener {
            startScan()
        }

        findViewById<android.widget.ImageButton>(R.id.btnRescan).setOnClickListener {
            startScan()
        }

        btnToggleSelectAll.setOnClickListener {
            isAllSelected = !isAllSelected
            adapter.selectAll(isAllSelected)
            btnToggleSelectAll.text = if (isAllSelected) getString(R.string.deselect_all) else getString(R.string.select_all)
            updateSummaryCard()
        }

        btnSort.setOnClickListener {
            isSortDescending = !isSortDescending
            if (isSortDescending) {
                appList.sortByDescending { it.cacheSizeBytes }
                btnSort.text = "Sort: Size ↓"
            } else {
                appList.sortBy { it.appName.lowercase() }
                btnSort.text = "Sort: Name ↑"
            }
            adapter.notifyDataSetChanged()
        }

        btnAutoClean.setOnClickListener {
            handleAutoCleanClick()
        }
    }

    private fun checkPermissionsAndScan() {
        if (!scanner.hasUsageStatsPermission()) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.permission_required)
                .setMessage(R.string.usage_stats_permission_msg)
                .setPositiveButton(R.string.grant_permission) { _, _ ->
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            data = Uri.parse("package:$packageName")
                        }
                    }
                    startActivity(intent)
                }
                .setNegativeButton(R.string.cancel) { _, _ ->
                    startScan()
                }
                .show()
        } else {
            startScan()
        }
    }

    private fun startScan() {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            val apps = scanner.scanInstalledApps(includeSystemApps = false)
            android.util.Log.d("MainActivity", "Scanned ${apps.size} apps successfully")
            appList.clear()
            appList.addAll(apps)
            adapter.updateList(apps)
            swipeRefresh.isRefreshing = false
            updateSummaryCard()
        }
    }

    private fun updateSummaryCard() {
        val totalBytes = adapter.getTotalCacheBytes()
        val selectedBytes = adapter.getSelectedCacheBytes()
        val selectedCount = adapter.getSelectedApps().size

        tvTotalCacheHeader.text = AppInfo.formatBytes(totalBytes)
        tvAppCount.text = "$selectedCount of ${appList.size} apps selected"
        tvSelectedCacheSize.text = "Selected: ${AppInfo.formatBytes(selectedBytes)}"

        if (totalBytes > 0) {
            val percentage = ((selectedBytes.toDouble() / totalBytes.toDouble()) * 100).toInt()
            progressBarCache.progress = percentage
        } else {
            progressBarCache.progress = 0
        }
    }

    private fun handleAutoCleanClick() {
        val selected = adapter.getSelectedApps()
        if (selected.isEmpty()) {
            Toast.makeText(this, "No apps selected to clean.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!CacheCleanerAccessibilityService.isServiceEnabled()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Enable Automation Service")
                .setMessage(R.string.accessibility_permission_msg)
                .setPositiveButton("Open Settings") { _, _ ->
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    startActivity(intent)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
            return
        }

        // Show progress dialog and start
        showCleaningDialog(selected)
    }

    private fun showCleaningDialog(selectedApps: List<AppInfo>) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_cleaning_progress, null)
        val tvCurrentApp = dialogView.findViewById<TextView>(R.id.tvCurrentCleaningApp)
        val pbCleaning = dialogView.findViewById<ProgressBar>(R.id.pbCleaning)
        val tvStats = dialogView.findViewById<TextView>(R.id.tvCleaningStats)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancelCleaning)

        pbCleaning.max = selectedApps.size
        pbCleaning.progress = 0

        progressDialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnCancel.setOnClickListener {
            CleaningManager.stopCleaning()
            progressDialog?.dismiss()
            Toast.makeText(this, "Cleaning canceled", Toast.LENGTH_SHORT).show()
        }

        CleaningManager.onProgressUpdate = { cleaned, total, currentApp ->
            pbCleaning.progress = cleaned
            tvStats.text = "$cleaned of $total apps cleaned"
            if (currentApp != null) {
                tvCurrentApp.text = "Cleaning: ${currentApp.appName}"
            }
        }

        CleaningManager.onCleaningFinished = { cleanedCount, _ ->
            progressDialog?.dismiss()
            Toast.makeText(this, "Done! Cleaned cache for $cleanedCount apps.", Toast.LENGTH_LONG).show()
            startScan()
        }

        progressDialog?.show()
        CleaningManager.startCleaning(this, selectedApps)
    }
}
