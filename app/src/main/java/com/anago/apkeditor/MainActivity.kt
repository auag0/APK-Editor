package com.anago.apkeditor

import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.anago.apkeditor.applist.AppListActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    
        val btnInstalledApp: MaterialButton = findViewById(R.id.btn_installedApp)
        val btnAPITable: MaterialButton = findViewById(R.id.btn_api_table)
    
        btnInstalledApp.setOnClickListener {
            val intent = Intent(this, AppListActivity::class.java)
            startActivity(intent)
        }
    
        btnAPITable.setOnClickListener {
            showAPITableDialog()
        }
    }
    
    private fun showAPITableDialog() {
        val webView = WebView(this).apply {
            loadUrl("file:///android_asset/apitable.html")
        }
        MaterialAlertDialogBuilder(this).apply {
            setView(webView)
        }.show()
    }
}