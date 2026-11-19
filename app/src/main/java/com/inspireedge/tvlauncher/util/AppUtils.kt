package com.inspireedge.tvlauncher.util

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.inspireedge.tvlauncher.model.AppInfo

object AppUtils {

    fun getInstalledApps(context: Context): List<AppInfo> {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)

        val apps = packageManager.queryIntentActivities(intent, 0)
            .sortedBy { it.activityInfo.loadLabel(packageManager).toString().lowercase() }
            .map { resolve ->
                val activityInfo = resolve.activityInfo
                val appInfo = activityInfo.applicationInfo

                // Skip system apps and launcher itself
                if (appInfo.packageName == context.packageName) return@map null

                val label = activityInfo.loadLabel(packageManager).toString()
                val icon = activityInfo.loadIcon(packageManager)

                AppInfo(
                    name = label,
                    icon = icon,
                    packageName = appInfo.packageName,
                    className = activityInfo.name
                )
            }
            .filterNotNull()
            .sortedBy { it.name }

        return apps
    }

    fun getSystemApps(context: Context): List<AppInfo> {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)

        return packageManager.queryIntentActivities(intent, 0)
            .filter { resolve ->
                val appInfo = resolve.activityInfo.applicationInfo
                appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
            }
            .map { resolve ->
                val activityInfo = resolve.activityInfo
                val appInfo = activityInfo.applicationInfo

                AppInfo(
                    name = activityInfo.loadLabel(packageManager).toString(),
                    icon = activityInfo.loadIcon(packageManager),
                    packageName = appInfo.packageName,
                    className = activityInfo.name
                )
            }
            .sortedBy { it.name }
    }

    fun createLaunchIntent(packageName: String, className: String): Intent? {
        return try {
            Intent().apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                setClassName(packageName, className)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        } catch (e: Exception) {
            null
        }
    }

    fun searchApps(apps: List<AppInfo>, query: String): List<AppInfo> {
        if (query.isBlank()) return apps

        val normalizedQuery = query.lowercase().trim()
        return apps.filter { app ->
            app.name.lowercase().contains(normalizedQuery) ||
            app.packageName.lowercase().contains(normalizedQuery)
        }
    }

    fun categorizeApps(apps: List<AppInfo>): List<Pair<String, List<AppInfo>>> {
        val categories = mutableMapOf<String, MutableList<AppInfo>>()

        apps.forEach { app ->
            val firstLetter = app.name.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
            categories.getOrPut(firstLetter) { mutableListOf() }.add(app)
        }

        return categories.map { (letter, categoryApps) ->
            letter to categoryApps.sortedBy { it.name }
        }.sortedBy { (letter, _) ->
            if (letter == "#") Int.MAX_VALUE else letter[0].code
        }
    }
}
