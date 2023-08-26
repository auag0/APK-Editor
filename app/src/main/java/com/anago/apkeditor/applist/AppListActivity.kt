package com.anago.apkeditor.applist

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anago.apkeditor.R
import me.zhanghai.android.fastscroll.FastScrollerBuilder

class AppListActivity : AppCompatActivity() {
    private val viewModel: AppListViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_applist)
        
        val appListAdapter = AppListAdapter(this)
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
}