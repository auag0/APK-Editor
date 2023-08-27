package com.anago.apkeditor.applist

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anago.apkeditor.compats.PackageManagerCompat.getCInstalledApplications
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
}