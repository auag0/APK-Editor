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
import kotlinx.coroutines.launch
import java.io.File

class APKEditViewModel(private val app: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(app) {
    private val appPackageName: String = savedStateHandle["packageName"] ?: throw ExceptionInInitializerError("")
    val applicationInfo: MutableLiveData<ApplicationInfo> = MutableLiveData(getAppInfo(appPackageName))
    private val decodedDir = File(app.filesDir, "decoded")
    var isExtracted: MutableLiveData<Boolean> = MutableLiveData(false)
    val isExtracting: MutableLiveData<Boolean> = MutableLiveData(false)
    val currentDir: MutableLiveData<File> = MutableLiveData(decodedDir)
    val fileList: MutableLiveData<List<File>> = MutableLiveData(emptyList())
    
    fun onFolderClicked(file: File) {
        if (file.isDirectory) {
            currentDir.value = file
            updateFileList()
        }
    }
    
    fun onBackDir() {
        val parentFile = currentDir.value?.parentFile ?: return
        if (!parentFile.startsWith(decodedDir)) {
            return
        }
        currentDir.value = parentFile
        updateFileList()
    }
    
    fun updateFileList() {
        fileList.value = currentDir.value?.listFiles()?.sortedWith(compareBy({ it.isFile }, { it.name }))
    }
    
    fun startUnZip() {
        if (isExtracted.value == true || isExtracting.value == true) {
            return
        }
        isExtracting.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val apkFile = getAPKPath()
            apkFile.unzip(decodedDir)
            isExtracting.postValue(false)
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