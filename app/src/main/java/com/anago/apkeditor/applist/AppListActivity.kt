package com.anago.apkeditor.applist

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anago.apkeditor.R
import com.anago.apkeditor.apkedit.APKEditActivity
import com.anago.apkeditor.compats.PackageManagerCompat.getCApplicationInfo
import com.anago.apkeditor.models.AppItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import java.io.File

class AppListActivity : AppCompatActivity(), AppListAdapter.Callback {
    private val viewModel: AppListViewModel by viewModels()
    
    private var selectedAppItem: AppItem? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_applist)
        
        val appListAdapter = AppListAdapter(this, this)
        val linearLayoutManager = LinearLayoutManager(this)
        findViewById<RecyclerView>(R.id.appList).apply {
            adapter = appListAdapter
            layoutManager = linearLayoutManager
            FastScrollerBuilder(this).useMd2Style().build()
        }
        
        viewModel.appList.observe(this) { newAppList ->
            appListAdapter.submitList(newAppList)
        }
        
        viewModel.loadAppList(this)
    }
    
    override fun onAppClicked(appItem: AppItem) {
        MaterialAlertDialogBuilder(this).apply {
            setTitle(appItem.name)
            setItems(arrayOf("APK Edit"), DialogInterface.OnClickListener { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(this@AppListActivity, APKEditActivity::class.java).apply {
                            putExtra("packageName", appItem.packageName)
                        }
                        startActivity(intent)
                    }
                }
            })
        }.show()
    }
    
    override fun onAppLongClicked(appItem: AppItem, itemView: View) {
        MaterialAlertDialogBuilder(this).apply {
            setTitle(appItem.name)
            setItems(arrayOf("Backup"), DialogInterface.OnClickListener { _, which ->
                when (which) {
                    0 -> {
                        selectedAppItem = appItem
                        selectedAppBackupApk.launch("${appItem.packageName}.apk")
                    }
                }
            })
        }.show()
    }
    
    private val selectedAppBackupApk = registerForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")) { uri ->
        lifecycleScope.launch(Dispatchers.Default) {
            val safeUri = uri ?: return@launch
            val selectedAppItem = selectedAppItem ?: return@launch
            
            val progressView = withContext(Dispatchers.Main) {
                LayoutInflater.from(this@AppListActivity).inflate(R.layout.view_progress, null, false)
            } as LinearProgressIndicator
            
            val dialog: AlertDialog = withContext(Dispatchers.Main) {
                MaterialAlertDialogBuilder(this@AppListActivity).apply {
                    setTitle("APK Backup")
                    setView(progressView)
                }.show()
            }
            
            val pm = packageManager
            val appInfo = pm.getCApplicationInfo(selectedAppItem.packageName, 0)
            val apkPath = appInfo.sourceDir
            val apkFile = File(apkPath)
            
            if (apkFile.exists() && apkFile.canRead()) {
                contentResolver.openOutputStream(safeUri)?.use { outputStream ->
                    val inputStream = apkFile.inputStream()
                    val fileSize = apkFile.length()
                    var bytesCopied: Long = 0
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var bytes = inputStream.read(buffer)
                    while (bytes >= 0) {
                        outputStream.write(buffer, 0, bytes)
                        bytesCopied += bytes
                        val progress = (bytesCopied.toFloat() / fileSize) * 100
                        withContext(Dispatchers.Main) {
                            progressView.progress = progress.toInt()
                        }
                        bytes = inputStream.read(buffer)
                    }
                }
                withContext(Dispatchers.Main) {
                    dialog.setTitle("Success")
                }
                delay(800)
            }
            withContext(Dispatchers.Main) {
                dialog.dismiss()
            }
        }
    }
}