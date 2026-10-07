package com.mrpowergamerbr.starfait.filesystem

class LayeredFileSystem(val saveFileSystem: FileSystem, val bundleFileSystem: FileSystem) {
    fun readFile(path: String) = saveFileSystem.readFile(path) ?: bundleFileSystem.readFile(path)
    fun writeFile(path: String, data: ByteArray) = saveFileSystem.writeFile(path, data)
    fun fileExists(path: String) = saveFileSystem.fileExists(path) || bundleFileSystem.fileExists(path)
}