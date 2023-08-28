package com.anago.apkeditor.apkedit

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.anago.apkeditor.R
import com.bumptech.glide.Glide
import com.google.android.material.textview.MaterialTextView
import java.io.File

class FileListAdapter(private val context: Context, private val callback: Callback) : ListAdapter<File, FileListAdapter.ViewHolder>(DiffCallback()) {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.image)
        val name: MaterialTextView = itemView.findViewById(R.id.name)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(context)
        val view = layoutInflater.inflate(R.layout.item_file, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = getItem(position)
        with(holder) {
            val imageDrawable = if (file.isDirectory) {
                R.drawable.ic_folder
            } else {
                R.drawable.ic_draft
            }
            Glide.with(context).load(imageDrawable).into(image)
            name.text = file.name
            
            itemView.setOnClickListener {
                callback.onFileClicked(file)
            }
        }
    }
    
    private class DiffCallback : DiffUtil.ItemCallback<File>() {
        override fun areItemsTheSame(oldItem: File, newItem: File): Boolean {
            return oldItem.absolutePath == newItem.absolutePath
        }
        
        override fun areContentsTheSame(oldItem: File, newItem: File): Boolean {
            return oldItem == newItem
        }
    }
    
    interface Callback {
        fun onFileClicked(file: File)
    }
}