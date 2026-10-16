package com.mrpowergamerbr.starfait.filesystem

abstract class FileSystem {
    abstract fun readFile(path: String): ByteArray?
    abstract fun writeFile(path: String, data: ByteArray)
    abstract fun fileExists(path: String): Boolean
}