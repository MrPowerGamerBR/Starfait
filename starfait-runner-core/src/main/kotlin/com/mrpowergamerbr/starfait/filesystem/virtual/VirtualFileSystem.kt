package com.mrpowergamerbr.starfait.filesystem.virtual

import com.mrpowergamerbr.starfait.filesystem.FileSystem

/**
 * A virtual file system where files are stored in memory
 */
class VirtualFileSystem : FileSystem() {
    val files = mutableMapOf<String, ByteArray>()

    override fun readFile(path: String): ByteArray? {
        return files[path]
    }

    override fun writeFile(path: String, data: ByteArray) {
        files[path] = data
    }

    override fun fileExists(path: String) = files.containsKey(path)
}