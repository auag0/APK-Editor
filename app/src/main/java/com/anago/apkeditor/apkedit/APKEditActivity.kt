package com.anago.apkeditor.apkedit

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anago.apkeditor.R
import com.anago.apkeditor.adapters.FileListAdapter
import com.anago.apkeditor.dialogs.FileSelectDialog
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import java.io.File

class APKEditActivity : AppCompatActivity(), FileListAdapter.Callback, FileSelectDialog.Callback {
    private val viewModel: APKEditViewModel by viewModels()
    
    private lateinit var iconImageView: ImageView
    private lateinit var nameTextView: MaterialTextView
    private lateinit var packageNameTextView: MaterialTextView
    private lateinit var progressLayout: LinearLayout
    private lateinit var progressTime: MaterialTextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnHome: ImageView
    private lateinit var btnAddFile: ImageView
    private lateinit var btnAddFolder: ImageView
    private lateinit var btnSmali: MaterialButton
    private lateinit var fileListAdapter: FileListAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_apkedit)
        
        initViews()
        setupRecyclerView()
        observeViewModel()
        setupBackHandle()
        setClickListeners()

        recyclerView.visibility = View.GONE
        progressLayout.visibility = View.VISIBLE

        viewModel.startUnZip()
    }

    private fun setupBackHandle() {
        onBackPressedDispatcher.addCallback(this) {
            if (!fileListAdapter.backDirectoryIfCan()) {
                finish()
            }
        }
    }
    
    private fun initViews() {
        iconImageView = findViewById(R.id.icon)
        nameTextView = findViewById(R.id.name)
        packageNameTextView = findViewById(R.id.packageName)
        progressLayout = findViewById(R.id.progressLayout)
        progressTime = progressLayout.findViewById(R.id.time)
        recyclerView = findViewById(R.id.recyclerView)
        btnHome = findViewById(R.id.btn_home)
        btnAddFile = findViewById(R.id.btn_addFile)
        btnAddFolder = findViewById(R.id.btn_addFolder)
        btnSmali = findViewById(R.id.btn_smali)
    }

    private fun setClickListeners() {
        btnHome.setOnClickListener {
            fileListAdapter.openDirectory(viewModel.decodedDir)
        }
        btnAddFile.setOnClickListener {
            val sdcard = Environment.getExternalStorageDirectory()
            val dialog = FileSelectDialog("Add a File", sdcard, false, this)
            dialog.show(supportFragmentManager, null)
        }
        btnAddFolder.setOnClickListener {
            val sdcard = Environment.getExternalStorageDirectory()
            val dialog = FileSelectDialog("Add a Folder", sdcard, true, this)
            dialog.show(supportFragmentManager, null)
        }
        btnSmali.setOnClickListener {
            viewModel.startDecompileAllSmali()
        }
    }
    
    private fun setupRecyclerView() {
        fileListAdapter = FileListAdapter(this, viewModel.decodedDir, null, this)
        val linearLayoutManager = LinearLayoutManager(this)
        with(recyclerView) {
            adapter = fileListAdapter
            layoutManager = linearLayoutManager
        }
    }
    
    private fun observeViewModel() {
        viewModel.applicationInfo.observe(this) { appInfo -> displayAppInfo(appInfo) }
        viewModel.isExtracted.observe(this) {
            if (it) {
                progressLayout.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                fileListAdapter.updateFileList()
            }
        }
        viewModel.progressTime.observe(this) {
            progressTime.text = "$it"
        }
        viewModel.isDecompiled.observe(this) {
            if (it) {
                runOnUiThread {
                    Toast.makeText(
                        this@APKEditActivity,
                        "Successfully decompiled dex files",
                        Toast.LENGTH_LONG
                    ).show()
                    btnSmali.isEnabled = false
                }
            }
        }
    }

    private fun displayAppInfo(appInfo: ApplicationInfo) {
        val pm = packageManager
        val appIcon = appInfo.loadIcon(pm)
        val appName = appInfo.loadLabel(pm).toString()
        val appPackageName = appInfo.packageName
        
        Glide.with(this).load(appIcon).into(iconImageView)
        nameTextView.text = appName
        packageNameTextView.text = appPackageName
    }

    override fun onFileClicked(file: File) {
        if (file.isDirectory) {
            fileListAdapter.openDirectory(file)
        } else {
            val fileUri = FileProvider.getUriForFile(this, "com.anago.apkeditor.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, contentResolver.getType(fileUri))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Open File"))
        }
    }

    override fun onSelectedFile(file: File) {
        val currentDir = fileListAdapter.getCurrentDir()
        if (file.isFile) {
            viewModel.addFile(file, currentDir) {
                fileListAdapter.updateFileList()
                runOnUiThread {
                    Toast.makeText(this, "copy successful.", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            viewModel.addFolder(file, currentDir) {
                fileListAdapter.updateFileList()
                runOnUiThread {
                    Toast.makeText(this, "copy successful.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}