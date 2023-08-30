package com.anago.apkeditor.apkedit

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.anago.apkeditor.compats.PackageManagerCompat.getCApplicationInfo
import com.anago.apkeditor.utils.FileUtils.unzip
import com.anago.apkeditor.utils.SmaliDecoder
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

    fun addFile(dest: File, to: File, finished: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            dest.copyTo(File(to, dest.name), true)
            finished?.invoke()
        }
    }

    fun addFolder(dest: File, to: File, finished: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            dest.copyRecursively(File(to, dest.name), true)
            finished?.invoke()
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

    val isDecompiled: MutableLiveData<Boolean> = MutableLiveData(false)
    private var isDecompiling: Boolean = false

    fun startDecompileAllSmali() {
        if (isDecompiled.value == true || isDecompiling) {
            return
        }
        isDecompiling = true
        viewModelScope.launch(Dispatchers.IO) {
            getDexFiles().forEach { dexFile ->
                val regex = Regex("classes(\\d*)\\.dex")
                val dexNum = regex.find(dexFile.name)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val outFileName = if (dexNum == 0) {
                    "smali"
                } else {
                    "smali_classes$dexNum"
                }
                val outFile = File(decodedDir, outFileName)
                SmaliDecoder.decode(getAPKPath(), outFile, dexFile.name, false, 0)
            }
            isDecompiling = false
            isDecompiled.postValue(true)
        }
    }

    private fun getDexFiles(): List<File> {
        return decodedDir.listFiles()?.filter {
            it.name.startsWith("classes") && it.name.endsWith(".dex")
        }?.sorted() ?: emptyList()
    }
    
    private fun getAppInfo(packageName: String): ApplicationInfo {
        val pm = app.packageManager
        return pm.getCApplicationInfo(packageName, 0)
    }
    
    private fun getAPKPath(): File {
        return File(getAppInfo(appPackageName).sourceDir)
    }
}