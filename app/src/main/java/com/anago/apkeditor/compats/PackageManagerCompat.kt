package com.anago.apkeditor.compats

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

object PackageManagerCompat {
    fun PackageManager.getCInstalledApplications(flags: Int): List<ApplicationInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getInstalledApplications(PackageManager.ApplicationInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION") getInstalledApplications(flags)
        }
    }
    
    fun PackageManager.getCApplicationInfo(packageName: String, flags: Int): ApplicationInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION") getApplicationInfo(packageName, flags)
        }
    }
}