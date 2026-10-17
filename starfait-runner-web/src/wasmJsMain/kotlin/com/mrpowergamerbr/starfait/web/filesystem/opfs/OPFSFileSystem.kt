package com.mrpowergamerbr.starfait.web.filesystem.opfs

import com.mrpowergamerbr.starfait.filesystem.FileSystem
import com.mrpowergamerbr.starfait.filesystem.virtual.VirtualFileSystem
import com.mrpowergamerbr.starfait.web.toArrayBufferFast
import js.objects.unsafeJso
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import web.fs.FileSystemDirectoryHandle
import web.fs.FileSystemGetFileOptions
import web.fs.FileSystemWriteChunkType
import web.fs.createWritable
import web.fs.getFileHandle
import web.fs.write
import web.navigator.navigator
import web.storage.getDirectory
import web.streams.close

class OPFSFileSystem(val virtualFileSystem: VirtualFileSystem, val opfsRoot: FileSystemDirectoryHandle) : FileSystem() {
    val mutex = Mutex()

    override fun readFile(path: String) = virtualFileSystem.readFile(path)

    override fun writeFile(path: String, data: ByteArray) {
        virtualFileSystem.writeFile(path, data)
        GlobalScope.launch {
            mutex.withLock {
                val handle = opfsRoot.getFileHandle(
                    path,
                    unsafeJso<FileSystemGetFileOptions>() {
                        this.create = true
                    }
                ).createWritable()
                handle.write(data.toArrayBufferFast())
                handle.close()
            }
        }
    }

    override fun fileExists(path: String) = virtualFileSystem.fileExists(path)
}