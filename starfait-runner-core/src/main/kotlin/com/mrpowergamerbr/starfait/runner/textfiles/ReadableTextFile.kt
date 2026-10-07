package com.mrpowergamerbr.starfait.runner.textfiles

import com.mrpowergamerbr.starfait.MathUtils.strtodStream

class ReadableTextFile(textFileId: Int, path: String, val content: ByteArray) : TextFile(textFileId, path) {
    // We DO IT OUR OWN WAY because we MUST support rewinds
    var position = 0

    override fun readString(): String {
        val builder = StringBuilder()

        while (true) {
            if (this.position == content.size)
                break

            val byte = this.content[this.position++].toInt()

            if (byte == '\n'.code) {
                this.position--
                break
            }

            builder.append(byte.toChar())
        }

        return builder.toString()
    }

    override fun readLine(): String {
        val builder = StringBuilder()

        while (true) {
            if (this.position == content.size)
                break

            val byte = this.content[this.position++].toInt()

            if (byte == '\n'.code) {
                break
            }

            builder.append(byte.toChar())
        }

        return builder.toString()
    }

    override fun readReal(): Double {
        return strtodStream {
            if (this.position == content.size)
                return@strtodStream -1

            this.content[position++].toInt()
        }
    }

    override fun writeString(value: String) = error("File is not open for writing")

    override fun hasRemaining() = this.position != this.content.size
}