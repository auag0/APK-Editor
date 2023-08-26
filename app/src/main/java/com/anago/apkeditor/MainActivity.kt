package com.anago.apkeditor

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.anago.apkeditor.applist.AppListActivity
import com.google.android.material.button.MaterialButton

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
    }
}