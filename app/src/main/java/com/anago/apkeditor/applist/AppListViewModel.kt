package com.anago.apkeditor.applist

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.anago.apkeditor.compats.PackageManagerCompat.getCInstalledApplications
import com.anago.apkeditor.models.AppItem
import com.anago.apkeditor.models.AppItem.Companion.toAppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppListViewModel(private val app: Application) : AndroidViewModel(app) {
    val appList: MutableLiveData<List<AppItem>> by lazy {
        MutableLiveData<List<AppItem>>(emptyList()).also {
            loadAppList(app)
        }
    }

    val isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    
    fun loadAppList(context: Context) {
        isLoading.value = true
        viewModelScope.launch(Dispatchers.Default) {
            val pm = context.packageManager
            val appInfoList = pm.getCInstalledApplications(0)
            val appItemList = appInfoList.map { it.toAppItem(pm) }
            val sortedAppItemList = appItemList.sortedWith(compareBy({ it.isSystem }, { it.name }))
            this@AppListViewModel.appList.postValue(sortedAppItemList)
        }
    }
}