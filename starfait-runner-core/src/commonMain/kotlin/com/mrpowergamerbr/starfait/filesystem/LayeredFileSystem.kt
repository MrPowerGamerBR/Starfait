package com.mrpowergamerbr.starfait.filesystem

class LayeredFileSystem(val saveFileSystem: FileSystem, val bundleFileSystem: FileSystem) {
    companion object {
        const val SAVE_FOLDER_PREFIX = "/save/"
        const val BUNDLE_FOLDER_PREFIX = "/bundle/"
    }

    fun readFile(path: String): ByteArray? {
        val (fileSystem, path) = selectFileSystemFromPath(path)
        return fileSystem?.readFile(path) ?: saveFileSystem.readFile(path) ?: bundleFileSystem.readFile(path)
    }

    fun writeFile(path: String, data: ByteArray) {
        val (fileSystem, path) = selectFileSystemFromPath(path)
        return fileSystem?.writeFile(path, data) ?: saveFileSystem.writeFile(path, data)
    }

    fun fileExists(path: String): Boolean {
        val (fileSystem, path) = selectFileSystemFromPath(path)
        return fileSystem?.fileExists(path) == true || saveFileSystem.fileExists(path) || bundleFileSystem.fileExists(path)
    }

    /**
     * Selects the [FileSystem] based on the [path]'s prefix
     *
     * If the path doesn't have a recognizable prefix, a null [FileSystem] will be returned on the result, which means that both file systems should be checked
     *
     * @param path the path
     */
    fun selectFileSystemFromPath(path: String): ExtractionResult {
        return if (path.startsWith(SAVE_FOLDER_PREFIX)) {
            ExtractionResult(this.saveFileSystem, path.removePrefix(SAVE_FOLDER_PREFIX))
        } else if (path.startsWith(BUNDLE_FOLDER_PREFIX)) {
            ExtractionResult(this.bundleFileSystem, path.removePrefix(BUNDLE_FOLDER_PREFIX))
        } else {
            ExtractionResult(null, path)
        }
    }

    data class ExtractionResult(
        val fileSystem: FileSystem?,
        val path: String
    )
}