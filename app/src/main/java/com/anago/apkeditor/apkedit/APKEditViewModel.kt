package com.anago.apkeditor.apkedit

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.anago.apkeditor.compats.PackageManagerCompat.getCApplicationInfo
import com.anago.apkeditor.utils.FileUtils.unzip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class APKEditViewModel(private val app: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(app) {
    private val appPackageName: String = savedStateHandle["packageName"] ?: throw ExceptionInInitializerError("")
    val applicationInfo: MutableLiveData<ApplicationInfo> = MutableLiveData(getAppInfo(appPackageName))
    val decodedDir = File(app.filesDir, "decoded")
    var isExtracted: MutableLiveData<Boolean> = MutableLiveData(false)
    private var isExtracting: Boolean = false
    val progressTime: MutableLiveData<Int> = MutableLiveData(0)

    fun addFile(dest: File, to: File) {
        viewModelScope.launch(Dispatchers.IO) {
            dest.copyTo(File(to, dest.name), true)
        }
    }

    private fun startProgressTimeAddLoop() {
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                if (!isExtracting) {
                    break
                }
                progressTime.postValue(((progressTime.value) ?: 0) + 1)
                delay(1000)
            }
        }
    }

    fun startUnZip() {
        if (isExtracted.value == true || isExtracting) {
            return
        }
        isExtracting = true
        progressTime.value = 0
        startProgressTimeAddLoop()
        viewModelScope.launch(Dispatchers.IO) {
            val apkFile = getAPKPath()
            apkFile.unzip(decodedDir)
            isExtracting = false
            isExtracted.postValue(true)
        }
    }
    
    private fun getAppInfo(packageName: String): ApplicationInfo {
        val pm = app.packageManager
        return pm.getCApplicationInfo(packageName, 0)
    }
    
    private fun getAPKPath(): File {
        return File(getAppInfo(appPackageName).sourceDir)
    }
}