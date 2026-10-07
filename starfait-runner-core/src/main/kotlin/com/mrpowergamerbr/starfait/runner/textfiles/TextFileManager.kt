package com.mrpowergamerbr.starfait.runner.textfiles

import com.mrpowergamerbr.starfait.filesystem.LayeredFileSystem

class TextFileManager(val layeredFileSystem: LayeredFileSystem) {
    // "You can only have a maximum of 32 files open at any one time.
    // You should also always close files when finished as this writes the information and frees the memory associated with the file."
    private val openFiles = arrayOfNulls<TextFile>(32)

    fun getTextFileById(fileId: Int) = this.openFiles[fileId - 1]

    fun openTextFile(filePath: String, write: Boolean): TextFile {
        val content = layeredFileSystem.readFile(filePath) ?: byteArrayOf()
        val allocatedId = allocateTextFileId()

        val file = if (write) {
            WritableTextFile(allocatedId, filePath)
        } else {
            ReadableTextFile(allocatedId, filePath, content)
        }

        this.openFiles[allocatedId - 1] = file
        return file
    }

    fun closeTextFile(fileId: Int) {
        val openFile = this.openFiles[fileId - 1]
        this.openFiles[fileId - 1] = null

        when (openFile) {
            is ReadableTextFile -> {}
            is WritableTextFile -> {
                openFile.outputStream.close()
                layeredFileSystem.writeFile(openFile.path, openFile.outputStream.toByteArray())
            }
            null -> TODO()
        }
    }

    private fun allocateTextFileId(): Int {
        for ((index, openFile) in this.openFiles.withIndex()) {
            if (openFile == null)
                return index + 1
        }

        error("All text file handles are in use!")
    }
}