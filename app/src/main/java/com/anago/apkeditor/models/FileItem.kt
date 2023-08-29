package com.anago.apkeditor.models

import java.io.File

data class FileItem(val name: String, val isDirectory: Boolean, val absolutePath: String) {
    companion object {
        fun FileItem.toFile(): File {
            return File(absolutePath)
        }
        
        fun File.toFileItem(): FileItem {
            return FileItem(
                name = this.name,
                isDirectory = this.isDirectory,
                absolutePath = this.absolutePath
            )
        }
    }
}