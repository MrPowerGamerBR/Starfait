package com.mrpowergamerbr.starfait.runner.textfiles

import java.io.ByteArrayOutputStream

class WritableTextFile(textFileId: Int, path: String) : TextFile(textFileId, path) {
    val outputStream = ByteArrayOutputStream()

    override fun writeString(value: String) {
        this.outputStream.write(value.toByteArray(Charsets.UTF_8))
    }

    override fun readString() = error("File is not open for reading")
    override fun readReal() = error("File is not open for reading")
    override fun readLine() = error("File is not open for reading")
    override fun hasRemaining() = error("File is not open for reading")
}