package com.inspireedge.tvlauncher

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.inspireedge.tvlauncher.model.AppInfo
import com.inspireedge.tvlauncher.ui.AppGridAdapter
import com.inspireedge.tvlauncher.ui.CategoryAdapter
import com.inspireedge.tvlauncher.ui.AppCategory
import com.inspireedge.tvlauncher.util.AppUtils

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var appAdapter: AppGridAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var searchView: TextView
    private lateinit var searchResults: RecyclerView
    private lateinit var searchLayoutManager: LinearLayoutManager
    private lateinit var searchAdapter: AppGridAdapter
    private var isSearchActive = false

    private val appList = mutableListOf<AppInfo>()
    private val categories = mutableListOf<AppCategory>()

    private val handler = Handler(Looper.getMainLooper())
    private val hideTimeout = 3000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Hide system UI for immersive experience
        hideSystemUI()

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView)
        searchView = findViewById(R.id.searchView)
        searchResults = findViewById(R.id.searchResults)
        searchLayoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        searchResults.layoutManager = searchLayoutManager

        // Setup main grid
        val gridLayoutManager = GridLayoutManager(this, 4)
        recyclerView.layoutManager = gridLayoutManager
        appAdapter = AppGridAdapter(emptyList()) { appInfo ->
            launchApp(appInfo)
        }
        recyclerView.adapter = appAdapter

        // Setup search results
        searchResults.layoutManager = searchLayoutManager
        searchAdapter = AppGridAdapter(emptyList()) { appInfo ->
            launchApp(appInfo)
            clearSearch()
        }
        searchResults.adapter = searchAdapter

        // Load apps
        loadApps()

        // Setup search
        setupSearch()

        // Auto-hide UI
        setupAutoHide()
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
    }

    private fun loadApps() {
        val packageManager = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        }

        val apps = packageManager.queryIntentActivities(intent, 0)
                .sortedBy { it.activityInfo.applicationInfo.loadLabel(packageManager).toString() }

        appList.clear()
        for (resolveInfo in apps) {
            val activityInfo = resolveInfo.activityInfo
            val appInfo = AppInfo(
                name = activityInfo.loadLabel(packageManager).toString(),
                icon = activityInfo.loadIcon(packageManager),
                packageName = activityInfo.packageName,
                className = activityInfo.name
            )
            appList.add(appInfo)
        }

        // Categorize apps
        categorizeApps()

        // Update UI
        updateAppGrid()
    }

    private fun categorizeApps() {
        categories.clear()

        val entertainmentApps = appList.filter { app ->
            val name = app.name.lowercase()
            name.contains("netflix") || name.contains("youtube") ||
                    name.contains("kodi") || name.contains("showmax") ||
                    name.contains("xyzplayer") || name.contains("playnet")
        }

        val utilityApps = appList.filter { app ->
            val name = app.name.lowercase()
            name.contains("settings") || name.contains("chrome") ||
                    name.contains("kodi") || name.contains("xyzplayer") ||
                    name.contains("ipvanish") || name.contains("footcric")
        }

        val systemApps = appList.filter { app ->
            val name = app.name.lowercase()
            name.contains("settings") || name.contains("gallery") ||
                    name.contains("documents") || name.contains("webview")
        }

        if (entertainmentApps.isNotEmpty()) {
            categories.add(AppCategory("Entertainment", entertainmentApps))
        }
        if (utilityApps.isNotEmpty()) {
            categories.add(AppCategory("Utilities", utilityApps))
        }
        if (systemApps.isNotEmpty()) {
            categories.add(AppCategory("System", systemApps))
        }

        // Add all apps as a fallback category
        if (appList.isNotEmpty()) {
            categories.add(AppCategory("All Apps", appList))
        }
    }

    private fun updateAppGrid() {
        if (categories.isEmpty()) {
            appAdapter.updateApps(emptyList())
            return
        }

        // Show first category by default
        val firstCategory = categories[0]
        appAdapter.updateApps(firstCategory.apps)

        // Setup category navigation
        setupCategoryNavigation()
    }

    private fun setupCategoryNavigation() {
        // Find category tabs
        val categoryTabs = findViewById<RecyclerView>(R.id.categoryTabs)
        if (categoryTabs != null) {
            val tabLayoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            categoryTabs.layoutManager = tabLayoutManager

            categoryAdapter = CategoryAdapter(categories) { category ->
                appAdapter.updateApps(category.apps)
            }
            categoryTabs.adapter = categoryAdapter
        }
    }

    private fun setupSearch() {
        searchView.setOnClickListener {
            startSearch()
        }

        searchView.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                startSearch()
                true
            } else {
                false
            }
        }
    }

    private fun startSearch() {
        isSearchActive = true
        searchView.text = ""
        searchResults.visibility = View.VISIBLE
        searchView.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun clearSearch() {
        isSearchActive = false
        searchResults.visibility = View.GONE
        searchView.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }

    private fun launchApp(appInfo: AppInfo) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error launching app: ${appInfo.name}", e)
        }
    }

    private fun setupAutoHide() {
        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                handler.postDelayed({
                    hideSystemUI()
                }, 1000)
            }
        }

        // Auto-hide after inactivity
        handler.postDelayed({
            hideSystemUI()
        }, hideTimeout)
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacksAndMessages(null)
        setupAutoHide()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                if (isSearchActive) {
                    clearSearch()
                    true
                } else {
                    super.onKeyDown(keyCode, event)
                }
            }
            KeyEvent.KEYCODE_SEARCH -> {
                startSearch()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        handler.removeCallbacksAndMessages(null)
        setupAutoHide()
    }

}
