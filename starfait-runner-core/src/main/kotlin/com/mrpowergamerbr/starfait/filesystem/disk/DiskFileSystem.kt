package com.mrpowergamerbr.starfait.filesystem.disk

import com.mrpowergamerbr.starfait.filesystem.FileSystem
import java.io.File

class DiskFileSystem(val folder: File) : FileSystem() {
    override fun readFile(path: String): ByteArray? {
        return folder.resolve(path).let { if (it.exists()) it.readBytes() else null }
    }

    override fun writeFile(path: String, data: ByteArray) {
        folder.resolve(path).writeBytes(data)
    }

    override fun fileExists(path: String) = folder.resolve(path).exists()
}