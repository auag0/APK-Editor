package com.anago.apkeditor.applist

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anago.apkeditor.models.AppItem
import com.anago.apkeditor.models.AppItem.Companion.toAppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppListViewModel : ViewModel() {
    val appList: MutableLiveData<List<AppItem>> by lazy {
        MutableLiveData<List<AppItem>>(emptyList())
    }
    
    fun loadAppList(context: Context) {
        viewModelScope.launch(Dispatchers.Default) {
            val pm = context.packageManager
            val appInfoList = pm.getCInstalledApplications(0)
            val appItemList = appInfoList.map { it.toAppItem(pm) }
            val sortedAppItemList = appItemList.sortedWith(compareBy({ it.isSystem }, { it.name }))
            this@AppListViewModel.appList.postValue(sortedAppItemList)
        }
    }
    
    private fun PackageManager.getCInstalledApplications(flags: Int): List<ApplicationInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getInstalledApplications(PackageManager.ApplicationInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION") getInstalledApplications(flags)
        }
    }
}