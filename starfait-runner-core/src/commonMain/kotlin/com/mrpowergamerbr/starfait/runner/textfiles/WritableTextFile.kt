package com.mrpowergamerbr.starfait.runner.textfiles

class WritableTextFile(textFileId: Int, path: String) : TextFile(textFileId, path) {
    companion object {
        private val INITIAL_SIZE = 8
    }

    var position = 0
    var output = ByteArray(INITIAL_SIZE)

    override fun writeString(value: String) {
        for (byte in value.encodeToByteArray()) {
            if (this.output.size == this.position) {
                this.output = this.output.copyOf(this.output.size * 2)
            }

            this.output[this.position++] = byte
        }
    }

    override fun readString() = error("File is not open for reading")
    override fun readReal() = error("File is not open for reading")
    override fun readLine() = error("File is not open for reading")
    override fun hasRemaining() = error("File is not open for reading")
}