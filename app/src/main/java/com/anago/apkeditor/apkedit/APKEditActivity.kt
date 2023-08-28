package com.anago.apkeditor.apkedit

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anago.apkeditor.R
import com.bumptech.glide.Glide
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textview.MaterialTextView
import java.io.File

class APKEditActivity : AppCompatActivity(), FileListAdapter.Callback {
    private val viewModel: APKEditViewModel by viewModels()
    
    private lateinit var iconImageView: ImageView
    private lateinit var nameTextView: MaterialTextView
    private lateinit var packageNameTextView: MaterialTextView
    private lateinit var progressView: CircularProgressIndicator
    private lateinit var recyclerView: RecyclerView
    private lateinit var fileListAdapter: FileListAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_apkedit)
        
        initViews()
        setupRecyclerView()
        observeViewModel()
        backHandle()
        
        viewModel.startUnZip()
    }
    
    private fun backHandle() {
        onBackPressedDispatcher.addCallback(this) {
            viewModel.onBackDir()
        }
    }
    
    private fun initViews() {
        iconImageView = findViewById(R.id.icon)
        nameTextView = findViewById(R.id.name)
        packageNameTextView = findViewById(R.id.packageName)
        progressView = findViewById(R.id.progress)
        recyclerView = findViewById(R.id.recyclerView)
    }
    
    private fun setupRecyclerView() {
        fileListAdapter = FileListAdapter(this, this)
        val linearLayoutManager = LinearLayoutManager(this)
        with(recyclerView) {
            adapter = fileListAdapter
            layoutManager = linearLayoutManager
        }
    }
    
    private fun observeViewModel() {
        viewModel.applicationInfo.observe(this) { appInfo -> displayAppInfo(appInfo) }
        viewModel.isExtracting.observe(this) {
            if (it) {
                progressView.visibility = View.VISIBLE
            } else {
                progressView.visibility = View.GONE
            }
        }
        viewModel.fileList.observe(this) {
            fileListAdapter.submitList(it)
        }
        viewModel.isExtracted.observe(this) {
            if (it) {
                viewModel.updateFileList()
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
            viewModel.onFolderClicked(file)
        }
    }
}