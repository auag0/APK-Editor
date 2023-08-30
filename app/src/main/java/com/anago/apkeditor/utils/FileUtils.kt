package com.anago.apkeditor.utils

import java.io.File
import java.io.FileFilter
import java.util.zip.ZipInputStream

object FileUtils {
    fun File.unzip(out: File, overwrite: Boolean = true, bufferSize: Int = DEFAULT_BUFFER_SIZE) {
        val buffer = ByteArray(bufferSize)
        
        if (out.exists() && overwrite) {
            out.deleteRecursively()
        }
        out.mkdirs()
        
        val zipFile = this
        val zipInputStream = ZipInputStream(zipFile.inputStream())
        while (true) {
            val entry = zipInputStream.nextEntry ?: break
            val entryFile = File(out, entry.name)
            if (entryFile.isDirectory) {
                entryFile.mkdirs()
            } else {
                entryFile.parentFile?.mkdirs()
                entryFile.outputStream().use { outputStream ->
                    var bytes = zipInputStream.read(buffer)
                    while (bytes >= 0) {
                        outputStream.write(buffer, 0, bytes)
                        bytes = zipInputStream.read(buffer)
                    }
                }
            }
        }
        zipInputStream.closeEntry()
        zipInputStream.close()
    }

    fun File.sortedFileList(filter: FileFilter? = null): List<File> {
        return listFiles(filter)?.sortedWith(compareBy({ it.isFile }, { it.name })) ?: emptyList()
    }
}