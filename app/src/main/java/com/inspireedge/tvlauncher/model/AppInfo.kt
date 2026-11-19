package com.inspireedge.tvlauncher.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val name: String,
    val icon: Drawable,
    val packageName: String,
    val className: String,
    val webUrl: String? = null
)
