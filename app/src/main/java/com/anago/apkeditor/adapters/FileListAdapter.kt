package com.anago.apkeditor.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.anago.apkeditor.R
import com.anago.apkeditor.models.FileItem
import com.anago.apkeditor.models.FileItem.Companion.toFile
import com.anago.apkeditor.models.FileItem.Companion.toFileItem
import com.anago.apkeditor.utils.FileUtils.sortedFileList
import com.bumptech.glide.Glide
import com.google.android.material.textview.MaterialTextView
import java.io.File
import java.io.FileFilter

class FileListAdapter(
    private val context: Context,
    private val rootDir: File,
    private val filter: FileFilter?,
    private val callback: Callback
) : RecyclerView.Adapter<FileListAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.image)
        val name: MaterialTextView = itemView.findViewById(R.id.name)
    }

    private var currentDir: File = rootDir
    private var fileList: List<FileItem> = currentDir.sortedFileList().map { it.toFileItem() }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(context)
        val view = layoutInflater.inflate(R.layout.item_file, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = fileList[position]
        with(holder) {
            val imageDrawable = if (file.name == "..") {
                R.drawable.ic_reply
            } else if (file.isDirectory) {
                R.drawable.ic_folder
            } else {
                R.drawable.ic_draft
            }
            Glide.with(context).load(imageDrawable).into(image)
            name.text = file.name
            
            itemView.setOnClickListener {
                val clickedFile = file.toFile()
                callback.onFileClicked(clickedFile)
            }
        }
    }

    override fun getItemCount(): Int {
        return fileList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateFileList() {
        fileList = currentDir.sortedFileList(filter).map { it.toFileItem() }.toMutableList().apply {
            val parentFile = currentDir.parentFile ?: return@apply
            if (parentFile.startsWith(rootDir)) {
                add(0, FileItem("..", true, parentFile.absolutePath))
            }
        }
        android.os.Handler(context.mainLooper).post {
            notifyDataSetChanged()
        }
    }

    fun openDirectory(file: File) {
        currentDir = file
        updateFileList()
    }

    fun backDirectoryIfCan(): Boolean {
        val parentFile = currentDir.parentFile ?: return false
        if (parentFile.startsWith(rootDir)) {
            currentDir = parentFile
            updateFileList()
            return true
        }
        return false
    }

    fun getCurrentDir(): File {
        return currentDir
    }

    interface Callback {
        fun onFileClicked(file: File)
    }
}