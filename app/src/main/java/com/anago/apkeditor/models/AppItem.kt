package com.anago.apkeditor.models

import android.content.pm.ApplicationInfo
import android.content.pm.ApplicationInfo.FLAG_SYSTEM
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class AppItem(val name: String, val packageName: String, val icon: Drawable, val isSystem: Boolean) {
    companion object {
        fun ApplicationInfo.toAppItem(pm: PackageManager): AppItem {
            return AppItem(name = loadLabel(pm).toString(), packageName = packageName, icon = loadIcon(pm), isSystem = flags and FLAG_SYSTEM != 0)
        }
    }
}