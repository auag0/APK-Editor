package com.anago.apkeditor.applist

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.anago.apkeditor.R
import com.anago.apkeditor.models.AppItem
import com.bumptech.glide.Glide
import com.google.android.material.textview.MaterialTextView

class AppListAdapter(private val context: Context) : ListAdapter<AppItem, AppListAdapter.ViewHolder>(DiffCallback()) {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.icon)
        val name: MaterialTextView = itemView.findViewById(R.id.name)
        val packageName: MaterialTextView = itemView.findViewById(R.id.packageName)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(context)
        val view = layoutInflater.inflate(R.layout.item_app, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appItem = getItem(position)
        with(holder) {
            val errorIcon = android.R.mipmap.sym_def_app_icon
            Glide.with(context).load(appItem.icon).error(errorIcon).into(icon)
            name.text = appItem.name
            packageName.text = appItem.packageName
        }
    }
    
    private class DiffCallback : DiffUtil.ItemCallback<AppItem>() {
        override fun areItemsTheSame(oldItem: AppItem, newItem: AppItem): Boolean {
            return oldItem.packageName == newItem.packageName
        }
        
        override fun areContentsTheSame(oldItem: AppItem, newItem: AppItem): Boolean {
            return oldItem == newItem
        }
    }
}