package com.cachecleaner.app.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cachecleaner.app.R
import com.cachecleaner.app.model.AppInfo
import com.google.android.material.checkbox.MaterialCheckBox

class AppAdapter(
    private var apps: MutableList<AppInfo>,
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cbSelect: MaterialCheckBox = view.findViewById(R.id.cbSelect)
        val ivAppIcon: ImageView = view.findViewById(R.id.ivAppIcon)
        val tvAppName: TextView = view.findViewById(R.id.tvAppName)
        val tvPackageName: TextView = view.findViewById(R.id.tvPackageName)
        val tvCacheSize: TextView = view.findViewById(R.id.tvCacheSize)
        val btnQuickOpen: ImageButton = view.findViewById(R.id.btnQuickOpen)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app_cache, parent, false)
        return AppViewHolder(view)
    }

    override fun getItemCount(): Int = apps.size

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]

        holder.tvAppName.text = app.appName
        holder.tvPackageName.text = app.packageName
        holder.tvCacheSize.text = app.formattedCacheSize()

        if (app.icon != null) {
            holder.ivAppIcon.setImageDrawable(app.icon)
        } else {
            holder.ivAppIcon.setImageResource(android.R.drawable.sym_def_app_icon)
        }

        // Set checked state without triggering listener during binding
        holder.cbSelect.setOnCheckedChangeListener(null)
        holder.cbSelect.isChecked = app.isSelected

        holder.cbSelect.setOnCheckedChangeListener { _, isChecked ->
            app.isSelected = isChecked
            onSelectionChanged()
        }

        holder.itemView.setOnClickListener {
            holder.cbSelect.isChecked = !holder.cbSelect.isChecked
        }

        // Quick open Settings directly for this app
        holder.btnQuickOpen.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${app.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            holder.itemView.context.startActivity(intent)
        }
    }

    fun updateList(newApps: List<AppInfo>) {
        if (apps !== newApps) {
            apps.clear()
            apps.addAll(newApps)
        }
        notifyDataSetChanged()
    }

    fun selectAll(select: Boolean) {
        for (app in apps) {
            app.isSelected = select
        }
        notifyDataSetChanged()
        onSelectionChanged()
    }

    fun getSelectedApps(): List<AppInfo> = apps.filter { it.isSelected }

    fun getTotalCacheBytes(): Long = apps.sumOf { it.cacheSizeBytes }

    fun getSelectedCacheBytes(): Long = apps.filter { it.isSelected }.sumOf { it.cacheSizeBytes }
}
